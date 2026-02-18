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
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;

@Service
public class RemarqueService {
    private final Map<Long, RemarqueDto> remarques = new LinkedHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final ClassRoomPlanService planService;

    public RemarqueService(ClassRoomPlanService planService) {
        this.planService = planService;
        seedSampleData();
    }

    public List<RemarqueDto> listAll() {
        return new ArrayList<>(remarques.values());
    }

    public RemarqueDto getById(long id) {
        return remarques.get(id);
    }

    public List<RemarqueDto> listByEleveId(long eleveId) {
        return remarques.values().stream()
            .filter(remarque -> remarque.eleveId() != null && remarque.eleveId() == eleveId)
            .toList();
    }

    public List<RemarqueDto> listByClassRoomId(long classRoomId) {
        return remarques.values().stream()
            .filter(remarque -> remarque.classRoomId() != null && remarque.classRoomId() == classRoomId)
            .toList();
    }

    public List<RemarqueDto> listByType(RemarqueType type) {
        return remarques.values().stream()
            .filter(remarque -> remarque.type() == type)
            .toList();
    }

    public List<RemarqueDto> listByEleveIdAndType(long eleveId, RemarqueType type) {
        return remarques.values().stream()
            .filter(remarque -> remarque.eleveId() != null && remarque.eleveId() == eleveId && remarque.type() == type)
            .toList();
    }

    public List<RemarqueDto> listByClassRoomIdAndType(long classRoomId, RemarqueType type) {
        return remarques.values().stream()
            .filter(remarque -> remarque.classRoomId() != null && remarque.classRoomId() == classRoomId && remarque.type() == type)
            .toList();
    }

    public RemarqueDto create(RemarqueRequest request) {
        return create(request, null);
    }

    public RemarqueDto create(RemarqueRequest request, RemarqueType forcedType) {
        long id = idGenerator.getAndIncrement();
        Instant now = Instant.now();
        RemarqueType type = forcedType != null ? forcedType : (request.type() != null ? request.type() : RemarqueType.REMARQUE_GENERALE);
        RemarqueDto created = new RemarqueDto(id, normalizeIntitule(request.intitule()), request.eleveId(), request.classRoomId(), type, now);
        remarques.put(id, created);
        return created;
    }

    public RemarqueDto update(long id, RemarqueRequest request) {
        RemarqueDto existing = remarques.get(id);
        if (existing == null) {
            return null;
        }

        String intitule = request.intitule() == null ? existing.intitule() : normalizeIntitule(request.intitule());
        Long eleveId = request.eleveId() == null ? existing.eleveId() : request.eleveId();
        Long classRoomId = request.classRoomId() == null ? existing.classRoomId() : request.classRoomId();
        RemarqueType type = request.type() == null ? existing.type() : request.type();

        RemarqueDto updated = new RemarqueDto(id, intitule, eleveId, classRoomId, type, existing.createdAt());
        remarques.put(id, updated);
        return updated;
    }

    public boolean delete(long id) {
        return remarques.remove(id) != null;
    }

    public int deleteByEleveId(long eleveId) {
        return deleteWhere(remarque -> remarque.eleveId() != null && remarque.eleveId() == eleveId);
    }

    public int deleteByEleveIds(List<Long> eleveIds) {
        if (eleveIds == null || eleveIds.isEmpty()) {
            return 0;
        }
        Set<Long> ids = new HashSet<>(eleveIds);
        return deleteWhere(remarque -> remarque.eleveId() != null && ids.contains(remarque.eleveId()));
    }

    public int deleteByClassRoomId(long classRoomId) {
        return deleteWhere(remarque -> remarque.classRoomId() != null && remarque.classRoomId() == classRoomId);
    }

    public RemarqueStats stats() {
        Map<Long, Integer> byEleve = new LinkedHashMap<>();
        Map<Long, Integer> byClassRoom = new LinkedHashMap<>();

        for (RemarqueDto remarque : remarques.values()) {
            if (remarque.eleveId() != null) {
                byEleve.merge(remarque.eleveId(), 1, Integer::sum);
            }
            if (remarque.classRoomId() != null) {
                byClassRoom.merge(remarque.classRoomId(), 1, Integer::sum);
            }
        }

        return new RemarqueStats(remarques.size(), byEleve, byClassRoom);
    }

    private String normalizeIntitule(String intitule) {
        return intitule == null ? null : intitule.trim();
    }

    private int deleteWhere(java.util.function.Predicate<RemarqueDto> predicate) {
        int removed = 0;
        var iterator = remarques.entrySet().iterator();
        while (iterator.hasNext()) {
            RemarqueDto remarque = iterator.next().getValue();
            if (predicate.test(remarque)) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    private void seedSampleData() {
        List<ClassRoom> classRooms = planService.getClassRooms();
        if (classRooms.isEmpty()) {
            return;
        }

        for (ClassRoom classRoom : classRooms) {
            List<Eleve> eleves = classRoom.getEleves().getEleves();
            // Remarques générales
            addSeedRemarque(classRoom.getId(), eleves, 0, "Participation active", RemarqueType.REMARQUE_GENERALE);
            addSeedRemarque(classRoom.getId(), eleves, 1, "Bon travail", RemarqueType.REMARQUE_GENERALE);
            
            // Devoirs non faits
            addSeedRemarque(classRoom.getId(), eleves, 0, "Devoir de mathématiques non rendu", RemarqueType.DEVOIR_NON_FAIT);
            addSeedRemarque(classRoom.getId(), eleves, 2, "Exercices page 42 non faits", RemarqueType.DEVOIR_NON_FAIT);
            
            // Bavardages
            addSeedRemarque(classRoom.getId(), eleves, 1, "Discussion pendant le cours", RemarqueType.BAVARDAGE);
            addSeedRemarque(classRoom.getId(), eleves, 3, "Bavardage répété", RemarqueType.BAVARDAGE);
        }
    }

    private void addSeedRemarque(long classRoomId, List<Eleve> eleves, int index, String intitule, RemarqueType type) {
        if (eleves.size() > index) {
            Eleve eleve = eleves.get(index);
            create(new RemarqueRequest(intitule, eleve.getId(), classRoomId, null), type);
        }
    }
}
