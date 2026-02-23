# ClassFlow-back
Le backend de l'appli ClassFlow (gestion de classe pour les professeurs)

## Authentification

### System multi-utilisateur
ClassFlow-back utilise Spring Security pour l'authentification. Chaque utilisateur a ses propres données (classes, élèves, remarques) complètement isolées.

### Connexion
**Credentials par défaut :**
- Username: `demo1` / Password: `demo1`
- Username: `demo2` / Password: `demo2`
- Username: `demo3` / Password: `demo3`

**Configuration des utilisateurs**  
Les utilisateurs sont définis dans [application.properties](src/main/resources/application.properties):
```properties
app.security.users=demo1:demo1:USER,demo2:demo2:USER,demo3:demo3:USER
```
Format: `username:password:ROLE[|ROLE2]`

### Utilisation de l'API

**Avec cURL (Basic Auth):**
```bash
curl -u demo1:demo1 http://localhost:8080/classrooms
```

**Avec Postman:**
- Authorization → Type: "Basic Auth"
- Username: `demo1`
- Password: `demo1`

**Avec navigateur:**
- Accéder à un endpoint protégé → redirection automatique vers `/login`
- Saisir username/password → cookie de session créé

### Endpoints publics (sans authentification)
- `GET /` `/login` `/version` `/ping`
- `GET /swagger` `/swagger-ui/**`
- `GET /actuator/health` `/actuator/info`

Tous les autres endpoints nécessitent une authentification.

## Persistance des données et isolation multi-utilisateur

### Base de données PostgreSQL
ClassFlow-back utilise **PostgreSQL** avec **Hibernate ORM** pour la persistance des données:
- **Configuration**: [application.properties](src/main/resources/application.properties)
- **Schéma auto-généré**: `spring.jpa.hibernate.ddl-auto=update` crée/met à jour les tables automatiquement
- **Tables principales**: `class_room`, `groupe`, `eleve`, `utilisateur`, `remarque`

### Isolation par propriétaire (Owner-Based Multi-Tenancy)
Chaque classe créée appartient à un utilisateur spécifique:
- Le champ `owner` de la table `class_room` stocke l'identifiant de l'utilisateur propriétaire
- Lors de la connexion, seules les classes de l'utilisateur connecté sont retournées
- **Exemple:**
  - `demo1` crée une classe → `owner = demo1`
  - `demo2` ne peut pas voir/modifier les classes de `demo1`
  - Chaque utilisateur voit uniquement ses propres données

### Sécurité
La propriété est vérifiée à chaque requête:
- Les queries utilisent `findByOwner(owner)` pour filtrer les données
- Les opérations (PUT, DELETE) vérifiaient la propriété avant d'exécuter l'action
- Impossible d'accéder aux données d'un autre utilisateur

# Liste des End Points

## Endpoints publics
GET /
GET /ping
GET /version

## Endpoints protégés (authentification requise)
GET /me
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
POST /classrooms/{id}/eleves/swap
POST /classrooms/{id}/groupes
POST /classrooms/{id}/groupes/aleatoire
PUT /classrooms/{id}
PUT /classrooms/{classRoomId}/eleves/{eleveId}
PUT /classrooms/{id}/tables/{tableIndex}/position
PUT /classrooms/{id}/eleves/{eleveId}/table/{tableIndex}
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

## Obtenir l'utilisateur connecté
GET /me
```json
{
	"username": "demo1",
	"roles": ["USER"]
}
```

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

## Mettre à jour la position d'une table
PUT /classrooms/{id}/tables/{tableIndex}/position
```json
{
	"x": 4,
	"y": 2
}
```

## Assigner un élève à une table
PUT /classrooms/{id}/eleves/{eleveId}/table/{tableIndex}
```json
{}
```

## Échanger deux élèves de place
POST /classrooms/{id}/eleves/swap
```json
{
	"eleveId1": 1,
	"eleveId2": 2
}
```

## Creer des groupes non aleatoires
POST /classrooms/{id}/groupes
```json
{
	"groupes": [[1, 2], [3, 4]],
	"noms": ["Groupe 1", "Groupe 2"]
}
```
La liste `noms` est optionnelle. Si omise, les groupes auront des noms par défaut.

## Creer des groupes aleatoires
POST /classrooms/{id}/groupes/aleatoire
```json
{
	"groupCount": 3
}
```

## Modifier un groupe (ajouter / retirer des eleves, changer le nom)
PUT /classrooms/{classRoomId}/groupes/{groupeId}
```json
{
	"addEleveIds": [5, 8],
	"removeEleveIds": [2],
	"nom": "Groupe des champions"
}
```
Tous les champs sont optionnels. Vous pouvez modifier uniquement le nom, uniquement les élèves, ou les deux.

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

---

## Isolation des données

**Important :** Chaque utilisateur authentifié a son propre espace de données :
- Les classes créées par `demo1` ne sont pas visibles par `demo2` ou `demo3`
- Les remarques d'un utilisateur sont complètement séparées des autres
- L'import/export CSV fonctionne uniquement sur les données de l'utilisateur connecté

Cette isolation garantit la confidentialité des données de chaque enseignant.
