# API d'authentification

- `POST /api/auth/register` — `{ matricule, nom, prenom, email, motDePasse, role? }`
- `POST /api/auth/login` — `{ email, motDePasse }`
- `PUT /api/users/{matricule}/profile` — `{ currentPassword, nom?, prenom?, email? }`
- `PUT /api/users/{matricule}/password` — `{ currentPassword, newPassword }`

Les mots de passe sont hashés avec PBKDF2-HMAC-SHA256 et un sel aléatoire. Les anciens mots de passe en clair sont migrés automatiquement lors d'une connexion réussie.
