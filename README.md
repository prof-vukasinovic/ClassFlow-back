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
GET /classrooms/{id}/chargement
POST /classrooms/sauvegarde
POST /classrooms/{id}/eleves
POST /classrooms/{id}/tables
DELETE /classrooms/{id}
DELETE /classrooms/{classRoomId}/eleves/{eleveId}
DELETE /classrooms/{classRoomId}/tables/{tableIndex}

GET /remarques
GET /remarques/{id}
GET /classrooms/{classRoomId}/remarques
GET /eleves/{eleveId}/remarques
GET /classrooms/{classRoomId}/eleves/{eleveId}/remarques
GET /remarques/stats
POST /remarques
PUT /remarques/{id}
DELETE /remarques/{id}
