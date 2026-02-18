# ClassFlow-back
Le backend de l'appli ClassFlow (gestion de classe pour les professeurs)

# Liste des End Points
GET /
GET /ping
GET /version

GET /classrooms
GET /classrooms/{id}
GET /classrooms/{id}/eleves
GET /classrooms/{id}/tables
GET /classrooms/{id}/plan
GET /classrooms/{id}/export-csv
GET /classrooms/{id}/groupes
POST /classrooms
POST /classrooms/import-csv
POST /classrooms/{id}/eleves
POST /classrooms/{id}/tables
POST /classrooms/{id}/groupes
POST /classrooms/{id}/groupes/aleatoire
PUT /classrooms/{id}
PUT /classrooms/{classRoomId}/eleves/{eleveId}
PUT /classrooms/{classRoomId}/groupes/{groupeId}
DELETE /classrooms/{id}
DELETE /classrooms/{classRoomId}/eleves/{eleveId}
DELETE /classrooms/{classRoomId}/tables/{tableIndex}
DELETE /classrooms/{classRoomId}/groupes/{groupeId}

GET /remarques
GET /remarques/{id}
GET /classrooms/{classRoomId}/remarques
GET /eleves/{eleveId}/remarques
GET /classrooms/{classRoomId}/eleves/{eleveId}/remarques
GET /remarques/stats
POST /remarques
PUT /remarques/{id}
DELETE /remarques/{id}

GET /devoirs-non-faits
GET /classrooms/{classRoomId}/devoirs-non-faits
GET /eleves/{eleveId}/devoirs-non-faits
POST /devoirs-non-faits

GET /bavardages
GET /classrooms/{classRoomId}/bavardages
GET /eleves/{eleveId}/bavardages
POST /bavardages

# Exemples de body

## Creer une classroom
POST /classrooms
```json
{
	"nom": "6èmeA"
}
```

## Modifier le nom d'une classroom
PUT /classrooms/{id}
```json
{
	"nom": "5èmeB"
}
```

## Importer une classroom depuis CSV
POST /classrooms/import-csv
```
nom,prenom
Durand,Alice
Martin,Pierre
Dupont,Sophie
```

## Exporter une classroom en CSV
GET /classrooms/{id}/export-csv
Retourne un fichier CSV avec les élèves de la classe.

## Creer un eleve
POST /classrooms/{id}/eleves
```json
{
	"nom": "Durand",
	"prenom": "Alice",
	"tableIndex": 1
}
```

## Modifier un eleve
PUT /classrooms/{classRoomId}/eleves/{eleveId}
```json
{
	"nom": "Martin",
	"prenom": "Pierre"
}
```

## Creer une table
POST /classrooms/{id}/tables
```json
{
	"x": 2,
	"y": 1
}
```

## Creer des groupes non aleatoires
POST /classrooms/{id}/groupes
```json
{
	"groupes": [[1, 2], [3, 4]]
}
```

## Creer des groupes aleatoires
POST /classrooms/{id}/groupes/aleatoire
```json
{
	"groupCount": 3
}
```

## Modifier un groupe (ajouter / retirer des eleves)
PUT /classrooms/{classRoomId}/groupes/{groupeId}
```json
{
	"addEleveIds": [5, 8],
	"removeEleveIds": [2]
}
```

## Remarques
POST /remarques
```json
{
	"intitule": "Participation active",
	"eleveId": 1,
	"classRoomId": 1,
	"type": "REMARQUE_GENERALE"
}
```

PUT /remarques/{id}
```json
{
	"intitule": "Bon travail",
	"eleveId": 1,
	"classRoomId": 1,
	"type": "REMARQUE_GENERALE"
}
```

## Devoirs non faits
POST /devoirs-non-faits
```json
{
	"intitule": "Devoir de mathématiques non rendu",
	"eleveId": 3,
	"classRoomId": 1
}
```

## Bavardages
POST /bavardages
```json
{
	"intitule": "Discussion pendant le cours",
	"eleveId": 5,
	"classRoomId": 1
}
```

**Types de remarques disponibles :**
- `REMARQUE_GENERALE` : remarque classique (par défaut)
- `DEVOIR_NON_FAIT` : devoir non rendu ou non fait
- `BAVARDAGE` : bavardage en classe
