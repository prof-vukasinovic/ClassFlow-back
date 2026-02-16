package com.eidd.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.eidd.DTO.ClassRoomExport;
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;
import com.eidd.model.Groupe;
import com.eidd.model.Position;
import com.eidd.model.Table;
import com.eidd.repositories.ClassRoomRespository;

@Service
public class ClassRoomPlanService {
    private final Map<Long, ClassRoom> classRooms = new LinkedHashMap<>();
    private final Map<Long, Map<Long, Groupe>> classRoomGroupes = new LinkedHashMap<>();
    private final ClassRoomService classRoomService = new ClassRoomService();
    private final GroupeService groupeService = new GroupeService();
    private final TableService tableService = new TableService();
    private long eleveIdCounter = 1;
    private long groupeIdCounter = 1;

    public ClassRoomPlanService() {
        seedSampleData();
    }

    public List<ClassRoom> getClassRooms() {
        return new ArrayList<>(classRooms.values());
    }

    public ClassRoom getClassRoom(long id) {
        return classRooms.get(id);
    }

    public boolean deleteClassRoom(long id) {
        classRoomGroupes.remove(id);
        return classRooms.remove(id) != null;
    }

    public List<GroupeEntry> createGroupesAleatoires(long classRoomId, int groupCount) {
        if (groupCount <= 0) {
            return null;
        }

        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return null;
        }

        List<Eleve> eleves = classRoom.getEleves().getEleves();
        if (eleves == null || eleves.isEmpty() || groupCount > eleves.size()) {
            return null;
        }

        Groupe pool = new Groupe();
        pool.setEleves(new ArrayList<>(eleves));

        int baseSize = eleves.size() / groupCount;
        int remainder = eleves.size() % groupCount;

        Map<Long, Groupe> groupes = classRoomGroupes.computeIfAbsent(classRoomId, id -> new LinkedHashMap<>());
        List<GroupeEntry> created = new ArrayList<>();

        for (int i = 0; i < groupCount; i++) {
            int size = baseSize + (i < remainder ? 1 : 0);
            Groupe groupe = new Groupe();

            for (int j = 0; j < size; j++) {
                Eleve eleve = groupeService.tirerEleveAuSort(pool);
                if (eleve == null) {
                    break;
                }
                groupeService.supprimerEleve(pool, eleve);
                groupeService.ajouterEleve(groupe, eleve);
            }

            long id = groupeIdCounter++;
            groupes.put(id, groupe);
            created.add(new GroupeEntry(id, groupe));
        }

        return created;
    }

    public List<GroupeEntry> createGroupes(long classRoomId, List<List<Long>> groupEleves) {
        if (groupEleves == null || groupEleves.isEmpty()) {
            return null;
        }

        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return null;
        }

        List<Eleve> eleves = classRoom.getEleves().getEleves();
        if (eleves == null || eleves.isEmpty()) {
            return null;
        }

        Map<Long, Eleve> eleveById = new HashMap<>();
        for (Eleve eleve : eleves) {
            eleveById.put(eleve.getId(), eleve);
        }

        Set<Long> used = new HashSet<>();
        Map<Long, Groupe> groupes = classRoomGroupes.computeIfAbsent(classRoomId, id -> new LinkedHashMap<>());
        List<GroupeEntry> created = new ArrayList<>();

        for (List<Long> ids : groupEleves) {
            if (ids == null || ids.isEmpty()) {
                return null;
            }

            Groupe groupe = new Groupe();
            for (Long eleveId : ids) {
                if (eleveId == null || !used.add(eleveId)) {
                    return null;
                }
                Eleve eleve = eleveById.get(eleveId);
                if (eleve == null) {
                    return null;
                }
                groupeService.ajouterEleve(groupe, eleve);
            }

            long id = groupeIdCounter++;
            groupes.put(id, groupe);
            created.add(new GroupeEntry(id, groupe));
        }

        return created;
    }

    public List<GroupeEntry> getGroupes(long classRoomId) {
        Map<Long, Groupe> groupes = classRoomGroupes.get(classRoomId);
        if (groupes == null) {
            return List.of();
        }
        List<GroupeEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, Groupe> entry : groupes.entrySet()) {
            entries.add(new GroupeEntry(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    public GroupeEntry updateGroupe(long classRoomId, long groupeId, List<Long> addEleveIds, List<Long> removeEleveIds) {
        Map<Long, Groupe> groupes = classRoomGroupes.get(classRoomId);
        if (groupes == null) {
            return null;
        }

        Groupe groupe = groupes.get(groupeId);
        if (groupe == null) {
            return null;
        }

        if ((addEleveIds == null || addEleveIds.isEmpty()) && (removeEleveIds == null || removeEleveIds.isEmpty())) {
            return null;
        }

        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return null;
        }

        List<Eleve> classEleves = classRoom.getEleves().getEleves();
        if (classEleves == null) {
            return null;
        }

        Map<Long, Eleve> eleveById = new HashMap<>();
        for (Eleve eleve : classEleves) {
            eleveById.put(eleve.getId(), eleve);
        }

        Set<Long> toAdd = addEleveIds == null ? Set.of() : new HashSet<>(addEleveIds);
        Set<Long> toRemove = removeEleveIds == null ? Set.of() : new HashSet<>(removeEleveIds);
        for (Long eleveId : toAdd) {
            if (eleveId == null || toRemove.contains(eleveId)) {
                return null;
            }
        }

        Map<Long, Eleve> current = new HashMap<>();
        for (Eleve eleve : groupe.getEleves()) {
            current.put(eleve.getId(), eleve);
        }

        for (Long eleveId : toRemove) {
            if (eleveId == null || !current.containsKey(eleveId)) {
                return null;
            }
            groupeService.supprimerEleve(groupe, current.get(eleveId));
        }

        for (Long eleveId : toAdd) {
            Eleve eleve = eleveById.get(eleveId);
            if (eleve == null || current.containsKey(eleveId)) {
                return null;
            }
            groupeService.ajouterEleve(groupe, eleve);
        }

        return new GroupeEntry(groupeId, groupe);
    }

    public boolean deleteGroupe(long classRoomId, long groupeId) {
        Map<Long, Groupe> groupes = classRoomGroupes.get(classRoomId);
        if (groupes == null) {
            return false;
        }
        Groupe removed = groupes.remove(groupeId);
        if (groupes.isEmpty()) {
            classRoomGroupes.remove(classRoomId);
        }
        return removed != null;
    }

    public boolean deleteEleve(long classRoomId, long eleveId) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return false;
        }
        List<Eleve> eleves = classRoom.getEleves().getEleves();
        if (eleves == null) {
            return false;
        }
        for (int i = 0; i < eleves.size(); i++) {
            Eleve eleve = eleves.get(i);
            if (eleve.getId() == eleveId) {
                eleves.remove(i);
                return true;
            }
        }
        return false;
    }

    public Eleve createEleve(long classRoomId, String nom, String prenom, Integer tableIndex) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null) {
            return null;
        }

        Groupe groupe = classRoom.getEleves();
        if (groupe == null) {
            groupe = new Groupe();
            classRoom.setEleves(groupe);
        }

        Eleve eleve = new Eleve(eleveIdCounter++, nom, prenom);
        if (tableIndex != null) {
            List<Table> tables = classRoom.getTables();
            if (tables == null || tableIndex < 0 || tableIndex >= tables.size()) {
                return null;
            }
            eleve.setTable(tables.get(tableIndex));
        }

        groupeService.ajouterEleve(groupe, eleve);
        return eleve;
    }

    public Table createTable(long classRoomId, int x, int y) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null) {
            return null;
        }

        List<Table> tables = classRoom.getTables();
        if (tables == null) {
            tables = new ArrayList<>();
            classRoom.setTables(tables);
        }

        Table table = tableService.creerTable(new Position(x, y));
        tables.add(table);
        return table;
    }

    public boolean deleteTableByIndex(long classRoomId, int tableIndex) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null || classRoom.getTables() == null) {
            return false;
        }
        List<Table> tables = classRoom.getTables();
        if (tableIndex < 0 || tableIndex >= tables.size()) {
            return false;
        }
        Table removed = tables.remove(tableIndex);
        if (classRoom.getEleves() != null) {
            for (Eleve eleve : classRoom.getEleves().getEleves()) {
                if (eleve.getTable() == removed) {
                    eleve.setTable(null);
                }
            }
        }
        return true;
    }


    public List<Eleve> getEleves(long id) {
        ClassRoom classRoom = classRooms.get(id);
        if (classRoom == null) {
            return List.of();
        }
        return classRoom.getEleves().getEleves();
    }

    public List<Table> getTables(long id) {
        ClassRoom classRoom = classRooms.get(id);
        if (classRoom == null) {
            return List.of();
        }
        return classRoom.getTables();
    }

    public ClassRoomExport exportClassRoom(long id) {
        ClassRoom classRoom = classRooms.get(id);
        if (classRoom == null) {
            return null;
        }
        return new ClassRoomExport(classRoom);
    }

    public ClassRoom saveClassRoom(ClassRoomExport export) {
        if (export == null) {
            return null;
        }
        ClassRoom classRoom = new ClassRoom(export);
        classRooms.put(classRoom.getId(), classRoom);
        updateCountersFromImport(classRoom);
        return classRoom;
    }

    private void seedSampleData() {
        ClassRoomRespository.resetCounter();
        ClassRoomRespository.incrementCounter();
        eleveIdCounter = 1;
        ClassRoom salleA = createClassRoom("Salle A", 3, 2);
        ClassRoom salleB = createClassRoom("Salle B", 2, 2);
        classRooms.put(salleA.getId(), salleA);
        classRooms.put(salleB.getId(), salleB);
    }

    private ClassRoom createClassRoom(String name, int width, int height) {
        ClassRoom classRoom = classRoomService.creerClassRoom(name);
        Groupe groupe = new Groupe();
        List<Table> tables = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Table table = tableService.creerTable(new Position(x, y));
                tables.add(table);

                long eleveId = eleveIdCounter++;
                Eleve eleve = new Eleve(eleveId, "Eleve" + eleveId, "Prenom" + eleveId);
                eleve.setTable(table);
                groupeService.ajouterEleve(groupe, eleve);
            }
        }

        classRoom.setEleves(groupe);
        classRoom.setTables(tables);
        return classRoom;
    }

    private void updateCountersFromImport(ClassRoom classRoom) {
        if (classRoom == null) {
            return;
        }

        long classRoomId = classRoom.getId();
        while (ClassRoomRespository.getCounter() <= classRoomId) {
            ClassRoomRespository.incrementCounter();
        }

        long maxEleveId = classRoom.getEleves() == null ? 0
            : classRoom.getEleves().getEleves().stream()
                .mapToLong(Eleve::getId)
                .max()
                .orElse(0);
        if (maxEleveId >= eleveIdCounter) {
            eleveIdCounter = maxEleveId + 1;
        }
    }
}
