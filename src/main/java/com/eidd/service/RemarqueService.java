package com.eidd.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.eidd.dto.RemarqueDto;
import com.eidd.dto.RemarqueRequest;
import com.eidd.dto.RemarqueStats;
import com.eidd.dto.RemarqueType;
@Service
public class RemarqueService {
    private static class OwnerRemarques {
        private final Map<Long, RemarqueDto> remarques = new LinkedHashMap<>();
        private final AtomicLong idGenerator = new AtomicLong(1);
    }

    private final Map<String, OwnerRemarques> remarquesByOwner = new LinkedHashMap<>();

    private String normalizeOwner(String owner) {
        return owner == null || owner.trim().isEmpty() ? "anonymous" : owner.trim();
    }

    private OwnerRemarques getOwnerRemarques(String owner) {
        String key = normalizeOwner(owner);
        return remarquesByOwner.computeIfAbsent(key, ignored -> new OwnerRemarques());
    }

    public List<RemarqueDto> listAll(String owner) {
        return new ArrayList<>(getOwnerRemarques(owner).remarques.values());
    }

    public RemarqueDto getById(String owner, long id) {
        return getOwnerRemarques(owner).remarques.get(id);
    }

    public List<RemarqueDto> listByEleveId(String owner, long eleveId) {
        return getOwnerRemarques(owner).remarques.values().stream()
            .filter(remarque -> remarque.eleveId() != null && remarque.eleveId() == eleveId)
            .toList();
    }

    public List<RemarqueDto> listByClassRoomId(String owner, long classRoomId) {
        return getOwnerRemarques(owner).remarques.values().stream()
            .filter(remarque -> remarque.classRoomId() != null && remarque.classRoomId() == classRoomId)
            .toList();
    }

    public List<RemarqueDto> listByType(String owner, RemarqueType type) {
        return getOwnerRemarques(owner).remarques.values().stream()
            .filter(remarque -> remarque.type() == type)
            .toList();
    }

    public List<RemarqueDto> listByEleveIdAndType(String owner, long eleveId, RemarqueType type) {
        return getOwnerRemarques(owner).remarques.values().stream()
            .filter(remarque -> remarque.eleveId() != null && remarque.eleveId() == eleveId && remarque.type() == type)
            .toList();
    }

    public List<RemarqueDto> listByClassRoomIdAndType(String owner, long classRoomId, RemarqueType type) {
        return getOwnerRemarques(owner).remarques.values().stream()
            .filter(remarque -> remarque.classRoomId() != null && remarque.classRoomId() == classRoomId && remarque.type() == type)
            .toList();
    }

    public RemarqueDto create(String owner, RemarqueRequest request) {
        return create(owner, request, null);
    }

    public RemarqueDto create(String owner, RemarqueRequest request, RemarqueType forcedType) {
        OwnerRemarques ownerRemarques = getOwnerRemarques(owner);
        long id = ownerRemarques.idGenerator.getAndIncrement();
        Instant now = Instant.now();
        RemarqueType type = forcedType != null ? forcedType : (request.type() != null ? request.type() : RemarqueType.REMARQUE_GENERALE);
        RemarqueDto created = new RemarqueDto(id, normalizeIntitule(request.intitule()), request.eleveId(), request.classRoomId(), type, now);
        ownerRemarques.remarques.put(id, created);
        return created;
    }

    public RemarqueDto update(String owner, long id, RemarqueRequest request) {
        OwnerRemarques ownerRemarques = getOwnerRemarques(owner);
        RemarqueDto existing = ownerRemarques.remarques.get(id);
        if (existing == null) {
            return null;
        }

        String intitule = request.intitule() == null ? existing.intitule() : normalizeIntitule(request.intitule());
        Long eleveId = request.eleveId() == null ? existing.eleveId() : request.eleveId();
        Long classRoomId = request.classRoomId() == null ? existing.classRoomId() : request.classRoomId();
        RemarqueType type = request.type() == null ? existing.type() : request.type();

        RemarqueDto updated = new RemarqueDto(id, intitule, eleveId, classRoomId, type, existing.createdAt());
        ownerRemarques.remarques.put(id, updated);
        return updated;
    }

    public boolean delete(String owner, long id) {
        return getOwnerRemarques(owner).remarques.remove(id) != null;
    }

    public int deleteByEleveId(String owner, long eleveId) {
        return deleteWhere(getOwnerRemarques(owner), remarque -> remarque.eleveId() != null && remarque.eleveId() == eleveId);
    }

    public int deleteByEleveIds(String owner, List<Long> eleveIds) {
        if (eleveIds == null || eleveIds.isEmpty()) {
            return 0;
        }
        Set<Long> ids = new HashSet<>(eleveIds);
        return deleteWhere(getOwnerRemarques(owner), remarque -> remarque.eleveId() != null && ids.contains(remarque.eleveId()));
    }

    public int deleteByClassRoomId(String owner, long classRoomId) {
        return deleteWhere(getOwnerRemarques(owner), remarque -> remarque.classRoomId() != null && remarque.classRoomId() == classRoomId);
    }

    public RemarqueStats stats(String owner) {
        Map<Long, Integer> byEleve = new LinkedHashMap<>();
        Map<Long, Integer> byClassRoom = new LinkedHashMap<>();

        for (RemarqueDto remarque : getOwnerRemarques(owner).remarques.values()) {
            if (remarque.eleveId() != null) {
                byEleve.merge(remarque.eleveId(), 1, Integer::sum);
            }
            if (remarque.classRoomId() != null) {
                byClassRoom.merge(remarque.classRoomId(), 1, Integer::sum);
            }
        }

        int count = getOwnerRemarques(owner).remarques.size();
        return new RemarqueStats(count, byEleve, byClassRoom);
    }

    private String normalizeIntitule(String intitule) {
        return intitule == null ? null : intitule.trim();
    }

    private int deleteWhere(OwnerRemarques ownerRemarques, java.util.function.Predicate<RemarqueDto> predicate) {
        int removed = 0;
        var iterator = ownerRemarques.remarques.entrySet().iterator();
        while (iterator.hasNext()) {
            RemarqueDto remarque = iterator.next().getValue();
            if (predicate.test(remarque)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

}
