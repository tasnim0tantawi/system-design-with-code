package com.booking.hotel.service;

import com.booking.common.dto.Cursor;
import com.booking.common.dto.CursorPageResponse;
import com.booking.common.exception.ApiException;
import com.booking.hotel.dto.HotelDtos.*;
import com.booking.hotel.entity.Hotel;
import com.booking.hotel.entity.Room;
import com.booking.hotel.entity.RoomAvailability;
import com.booking.hotel.external.HotelVerificationApi;
import com.booking.hotel.external.ObjectStorage;
import com.booking.hotel.repository.HotelRepository;
import com.booking.hotel.repository.RoomAvailabilityRepository;
import com.booking.hotel.repository.RoomRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class HotelService {

    private final HotelRepository hotelRepo;
    private final RoomRepository roomRepo;
    private final RoomAvailabilityRepository availRepo;
    private final HotelVerificationApi verifier;
    private final ObjectStorage storage;
    private final ReactiveStringRedisTemplate redis;
    private final HotelEventProducer events;
    private final ObjectMapper json = new ObjectMapper();

    public HotelService(HotelRepository hotelRepo, RoomRepository roomRepo,
                        RoomAvailabilityRepository availRepo, HotelVerificationApi verifier,
                        ObjectStorage storage, ReactiveStringRedisTemplate redis,
                        HotelEventProducer events) {
        this.hotelRepo = hotelRepo;
        this.roomRepo = roomRepo;
        this.availRepo = availRepo;
        this.verifier = verifier;
        this.storage = storage;
        this.redis = redis;
        this.events = events;
    }

    public Mono<HotelResponse> createHotel(long managerId, CreateHotelRequest req) {
        return verifier.verify(req.businessCredentials())
                .flatMap(verified -> {
                    Hotel h = new Hotel();
                    h.setManagerId((int) managerId);
                    h.setName(req.name());
                    h.setDescription(req.description());
                    h.setLocation(req.location());
                    h.setStars(req.stars());
                    h.setType(req.type());
                    h.setStatus(verified ? "verified" : "pending_verification");
                    h.setCreatedAt(Instant.now());
                    return hotelRepo.save(h);
                })
                .map(this::toResponse)
                .flatMap(resp -> events.sendCreated(resp).thenReturn(resp));
    }

    public Mono<HotelResponse> getHotel(int id) {
        String cacheKey = "hotel:" + id;
        return redis.opsForValue().get(cacheKey)
                .flatMap(this::deserialize)
                .switchIfEmpty(
                        hotelRepo.findById(id)
                                .switchIfEmpty(Mono.error(ApiException.notFound("hotel " + id)))
                                .map(this::toResponse)
                                .flatMap(resp -> serialize(resp)
                                        .flatMap(s -> redis.opsForValue().set(cacheKey, s, Duration.ofSeconds(60)))
                                        .thenReturn(resp))
                );
    }

    public Mono<HotelResponse> updateHotel(int id, long managerId, CreateHotelRequest req) {
        return hotelRepo.findById(id)
                .switchIfEmpty(Mono.error(ApiException.notFound("hotel " + id)))
                .flatMap(h -> {
                    if (h.getManagerId() != (int) managerId) {
                        return Mono.error(ApiException.forbidden("not your hotel"));
                    }
                    h.setName(req.name());
                    h.setDescription(req.description());
                    h.setLocation(req.location());
                    h.setStars(req.stars());
                    h.setType(req.type());
                    return hotelRepo.save(h);
                })
                .flatMap(saved -> redis.delete("hotel:" + id).thenReturn(saved))
                .map(this::toResponse)
                .flatMap(resp -> events.sendUpdated(resp).thenReturn(resp));
    }

    public Mono<RoomResponse> addRoom(int hotelId, long managerId, CreateRoomRequest req) {
        return hotelRepo.findById(hotelId)
                .switchIfEmpty(Mono.error(ApiException.notFound("hotel " + hotelId)))
                .flatMap(h -> {
                    if (h.getManagerId() != (int) managerId) {
                        return Mono.error(ApiException.forbidden("not your hotel"));
                    }
                    Room r = new Room();
                    r.setHotelId(hotelId);
                    r.setRoomType(req.roomType());
                    r.setBasePrice(req.basePrice());
                    r.setTotalCount(req.totalCount());
                    return roomRepo.save(r);
                })
                .flatMap(saved -> seedAvailability(saved).thenReturn(saved))
                .map(r -> new RoomResponse(r.getId(), r.getHotelId(), r.getRoomType(),
                        r.getBasePrice(), r.getTotalCount()));
    }

    /**
     * Seed 365 days of availability rows so /availability has something to read.
     * In production this would happen via a scheduled job + on-demand backfill.
     */
    private Mono<Void> seedAvailability(Room r) {
        LocalDate today = LocalDate.now();
        List<RoomAvailability> rows = new ArrayList<>();
        for (int i = 0; i < 365; i++) {
            RoomAvailability ra = new RoomAvailability();
            ra.setRoomId(r.getId());
            ra.setDate(today.plusDays(i));
            ra.setAvailableCount(r.getTotalCount());
            rows.add(ra);
        }
        return availRepo.saveAll(rows).then();
    }

    public Mono<AvailabilityResponse> getAvailability(int hotelId, LocalDate checkIn, LocalDate checkOut) {
        if (!checkOut.isAfter(checkIn)) {
            return Mono.error(ApiException.badRequest("check_out must be after check_in"));
        }
        long nights = checkOut.toEpochDay() - checkIn.toEpochDay();
        return roomRepo.findByHotelId(hotelId)
                .flatMap(room -> availRepo.findByRoomIdAndDateBetween(room.getId(), checkIn, checkOut.minusDays(0))
                        .collectList()
                        .map(list -> {
                            int min = list.stream()
                                    .filter(ra -> !ra.getDate().isBefore(checkIn) && ra.getDate().isBefore(checkOut))
                                    .mapToInt(RoomAvailability::getAvailableCount)
                                    .min().orElse(0);
                            // Simple "dynamic" pricing: +20% if booking is within 7 days from today
                            BigDecimal multiplier = checkIn.isBefore(LocalDate.now().plusDays(7))
                                    ? BigDecimal.valueOf(1.20) : BigDecimal.ONE;
                            BigDecimal pricePerNight = room.getBasePrice().multiply(multiplier)
                                    .setScale(2, RoundingMode.HALF_UP);
                            BigDecimal total = pricePerNight.multiply(BigDecimal.valueOf(nights));
                            return new RoomAvailabilityItem(room.getId(), room.getRoomType(),
                                    pricePerNight, total, min);
                        }))
                .collectList()
                .map(items -> new AvailabilityResponse(hotelId, checkIn, checkOut, items));
    }

    public Mono<Void> decrementAvailability(DecrementRequest req) {
        return availRepo.decrement(req.roomId(), req.checkIn(), req.checkOut()).then();
    }

    public Mono<PresignResponse> presign(String contentType) {
        return storage.presign(contentType)
                .map(p -> new PresignResponse(p.key(), p.publicUrl(), p.uploadUrl()));
    }

    private HotelResponse toResponse(Hotel h) {
        return new HotelResponse(h.getId(), h.getManagerId(), h.getName(), h.getDescription(),
                h.getLocation(), h.getStars(), h.getType(), h.getStatus());
    }

    private Mono<String> serialize(HotelResponse h) {
        try {
            return Mono.just(json.writeValueAsString(h));
        } catch (JsonProcessingException e) {
            return Mono.error(e);
        }
    }

    private Mono<HotelResponse> deserialize(String s) {
        try {
            return Mono.just(json.readValue(s, HotelResponse.class));
        } catch (Exception e) {
            return Mono.empty();
        }
    }

    public Flux<HotelResponse> listAll() {
        return hotelRepo.findAll().map(this::toResponse);
    }

    /**
     * Cursor-paged hotel listing. Fetches limit+1 rows so we can detect
     * "is there a next page?" without a separate count query.
     */
    public Mono<CursorPageResponse<HotelResponse>> listPage(String cursor, int limit) {
        long after = Cursor.decode(cursor);
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return hotelRepo.findPage(after, safeLimit + 1)
                .map(this::toResponse)
                .collectList()
                .map(items -> {
                    boolean hasMore = items.size() > safeLimit;
                    var page = hasMore ? items.subList(0, safeLimit) : items;
                    String next = hasMore ? Cursor.encode(page.get(page.size() - 1).id()) : null;
                    return CursorPageResponse.of(page, next, hasMore);
                });
    }
}
