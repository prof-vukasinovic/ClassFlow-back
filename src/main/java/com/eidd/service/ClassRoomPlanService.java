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
import com.eidd.repositories.ClassRoomRepository;

@Service
public class ClassRoomPlanService {
    private final Map<String, Map<Long, Map<Long, Groupe>>> classRoomGroupesByOwner = new LinkedHashMap<>();
    private final ClassRoomRepository classRoomRepository;
    private final ClassRoomService classRoomService;
    private final GroupeService groupeService;
    private final TableService tableService;
    private long groupeIdCounter = 1;

    public ClassRoomPlanService(ClassRoomRepository classRoomRepository,
            ClassRoomService classRoomService,
            GroupeService groupeService,
            TableService tableService) {
        this.classRoomRepository = classRoomRepository;
        this.classRoomService = classRoomService;
        this.groupeService = groupeService;
        this.tableService = tableService;
    }

    private String normalizeOwner(String owner) {
        return owner == null || owner.trim().isEmpty() ? "anonymous" : owner.trim();
    }

    private ClassRoom findOwnedClassRoom(String owner, long id) {
        String key = normalizeOwner(owner);
        return classRoomRepository.findByIdAndOwner(id, key).orElse(null);
    }

    private Map<Long, Map<Long, Groupe>> getGroupStore(String owner) {
        String key = normalizeOwner(owner);
        return classRoomGroupesByOwner.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
    }

    public List<ClassRoom> getClassRooms(String owner) {
        return classRoomRepository.findByOwner(normalizeOwner(owner));
    }

    public ClassRoom getClassRoom(String owner, long id) {
        return findOwnedClassRoom(owner, id);
    }

    public boolean deleteClassRoom(String owner, long id) {
        getGroupStore(owner).remove(id);
        ClassRoom classRoom = findOwnedClassRoom(owner, id);
        if (classRoom == null) {
            return false;
        }
        classRoomRepository.delete(classRoom);
        return true;
    }

    public ClassRoom createNewClassRoom(String owner, String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return null;
        }
        ClassRoomExport export = classRoomService.creerClassRoom(nom, normalizeOwner(owner));
        if (export == null) {
            return null;
        }
        ClassRoom saved = findOwnedClassRoom(owner, export.getId());
        if (saved != null) {
            return saved;
        }
        ClassRoom fallback = new ClassRoom(export);
        fallback.setOwner(normalizeOwner(owner));
        return fallback;
    }

    public List<GroupeEntry> createGroupesAleatoires(String owner, long classRoomId, int groupCount) {
        if (groupCount <= 0) {
            return null;
        }

        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
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

        Map<Long, Groupe> groupes = getGroupStore(owner).computeIfAbsent(classRoomId, id -> new LinkedHashMap<>());
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

    public List<GroupeEntry> createGroupes(String owner, long classRoomId, List<List<Long>> groupEleves, List<String> noms) {
        if (groupEleves == null || groupEleves.isEmpty()) {
            return null;
        }

        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
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
        Map<Long, Groupe> groupes = getGroupStore(owner).computeIfAbsent(classRoomId, id -> new LinkedHashMap<>());
        List<GroupeEntry> created = new ArrayList<>();

        for (int i = 0; i < groupEleves.size(); i++) {
            List<Long> ids = groupEleves.get(i);
            if (ids == null || ids.isEmpty()) {
                return null;
            }

            Groupe groupe = new Groupe();
            
            // Assigner le nom si fourni
            if (noms != null && i < noms.size() && noms.get(i) != null) {
                groupe.setNom(noms.get(i));
            }
            
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

    public List<GroupeEntry> getGroupes(String owner, long classRoomId) {
        Map<Long, Groupe> groupes = getGroupStore(owner).get(classRoomId);
        if (groupes == null) {
            return List.of();
        }
        List<GroupeEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, Groupe> entry : groupes.entrySet()) {
            entries.add(new GroupeEntry(entry.getKey(), entry.getValue()));
        }
        return entries;
    }

    public GroupeEntry updateGroupe(String owner, long classRoomId, long groupeId, List<Long> addEleveIds, List<Long> removeEleveIds, String nom) {
        Map<Long, Groupe> groupes = getGroupStore(owner).get(classRoomId);
        if (groupes == null) {
            return null;
        }

        Groupe groupe = groupes.get(groupeId);
        if (groupe == null) {
            return null;
        }

        // Si seulement le nom est fourni, on peut le mettre à jour
        if ((addEleveIds == null || addEleveIds.isEmpty()) && (removeEleveIds == null || removeEleveIds.isEmpty())) {
            if (nom != null) {
                groupe.setNom(nom);
                return new GroupeEntry(groupeId, groupe);
            }
            return null;
        }

        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
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

        // Mettre à jour le nom si fourni
        if (nom != null) {
            groupe.setNom(nom);
        }

        return new GroupeEntry(groupeId, groupe);
    }

    public boolean deleteGroupe(String owner, long classRoomId, long groupeId) {
        Map<Long, Groupe> groupes = getGroupStore(owner).get(classRoomId);
        if (groupes == null) {
            return false;
        }
        Groupe removed = groupes.remove(groupeId);
        if (groupes.isEmpty()) {
            getGroupStore(owner).remove(classRoomId);
        }
        return removed != null;
    }

    public boolean deleteEleve(String owner, long classRoomId, long eleveId) {
        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
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
                classRoomRepository.save(classRoom);
                return true;
            }
        }
        return false;
    }

    public Eleve createEleve(String owner, long classRoomId, String nom, String prenom, Integer tableIndex) {
        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
        if (classRoom == null) {
            return null;
        }

        Groupe groupe = classRoom.getEleves();
        if (groupe == null) {
            groupe = new Groupe();
            classRoom.setEleves(groupe);
        }

        Eleve eleve = new Eleve(nom, prenom);
        if (tableIndex != null) {
            List<Table> tables = classRoom.getTables();
            if (tables == null || tableIndex < 0 || tableIndex >= tables.size()) {
                return null;
            }
            eleve.setTable(tables.get(tableIndex));
        }

        groupeService.ajouterEleve(groupe, eleve);
        classRoomRepository.save(classRoom);
        return eleve;
    }

    public Eleve updateEleve(String owner, long classRoomId, long eleveId, String nom, String prenom) {
        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return null;
        }

        List<Eleve> eleves = classRoom.getEleves().getEleves();
        if (eleves == null) {
            return null;
        }

        for (Eleve eleve : eleves) {
            if (eleve.getId() == eleveId) {
                if (nom != null && !nom.trim().isEmpty()) {
                    eleve.setNom(nom);
                }
                if (prenom != null && !prenom.trim().isEmpty()) {
                    eleve.setPrenom(prenom);
                }
                classRoomRepository.save(classRoom);
                return eleve;
            }
        }

        return null;
    }

    public ClassRoom updateClassRoom(String owner, long classRoomId, String nom) {
        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
        if (classRoom == null) {
            return null;
        }

        if (nom == null || nom.trim().isEmpty()) {
            return null;
        }

        classRoom.setNom(nom);
        return classRoomRepository.save(classRoom);
    }

    public Table createTable(String owner, long classRoomId, int x, int y) {
        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
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
        classRoomRepository.save(classRoom);
        return table;
    }

    public boolean deleteTableByIndex(String owner, long classRoomId, int tableIndex) {
        ClassRoom classRoom = findOwnedClassRoom(owner, classRoomId);
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
        classRoomRepository.save(classRoom);
        return true;
    }


    public List<Eleve> getEleves(String owner, long id) {
        ClassRoom classRoom = findOwnedClassRoom(owner, id);
        if (classRoom == null) {
            return List.of();
        }
        return classRoom.getEleves() == null ? List.of() : classRoom.getEleves().getEleves();
    }

    public List<Table> getTables(String owner, long id) {
        ClassRoom classRoom = findOwnedClassRoom(owner, id);
        if (classRoom == null) {
            return List.of();
        }
        return classRoom.getTables() == null ? List.of() : classRoom.getTables();
    }

    public ClassRoomExport exportClassRoom(String owner, long id) {
        ClassRoom classRoom = findOwnedClassRoom(owner, id);
        if (classRoom == null) {
            return null;
        }
        return new ClassRoomExport(classRoom);
    }

    public ClassRoom saveClassRoom(String owner, ClassRoomExport export) {
        if (export == null) {
            return null;
        }
        ClassRoom classRoom = new ClassRoom(export);
        classRoom.setOwner(normalizeOwner(owner));
        return classRoomRepository.save(classRoom);
    }

    public ClassRoom importFromCsv(String owner, String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return null;
        }

        String normalizedCsv = normalizeCsvContent(csvContent);
        ClassRoomExport export = CsvService.importFromCsv(normalizedCsv);
        if (export == null) {
            return null;
        }

        export.setNom(unquoteCsvValue(export.getNom()));
        
        ClassRoom classRoom = new ClassRoom(export);
        classRoom.setOwner(normalizeOwner(owner));
        return classRoomRepository.save(classRoom);
    }

    public String exportToCsv(String owner, long id) {
        ClassRoom classRoom = findOwnedClassRoom(owner, id);
        if (classRoom == null) {
            return null;
        }
        
        ClassRoomExport export = new ClassRoomExport(classRoom);
        return CsvService.exportToCsv(export);
    }

    private String normalizeCsvContent(String csvContent) {
        String trimmed = csvContent.trim();
        if (trimmed.isEmpty()) {
            return csvContent;
        }

        String[] lines = trimmed.split("\\r?\\n");
        if (lines.length == 0) {
            return csvContent;
        }

        String firstLine = lines[0].trim();
        if (firstLine.contains(";")) {
            return csvContent;
        }

        long classRoomId = 0;

        StringBuilder builder = new StringBuilder();
        builder.append(classRoomId)
            .append(";\"")
            .append(escapeCsvValue(firstLine))
            .append("\"\n");

        long eleveId = 1;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts.length < 2) {
                parts = line.split(";", -1);
            }
            if (parts.length < 2) {
                continue;
            }

            String nom = parts[0].trim();
            String prenom = parts[1].trim();
            builder.append(eleveId++)
                .append(";\"")
                .append(escapeCsvValue(nom))
                .append("\";\"")
                .append(escapeCsvValue(prenom))
                .append("\"\n");
        }

        return builder.toString();
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\"", "\"\"");
    }

    private String unquoteCsvValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }

}
