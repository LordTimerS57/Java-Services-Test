# Authentification et compte utilisateur

Les endpoints sont préfixés par `/api` via `web.xml`.

- `POST /api/auth/register`: `{ matricule, nom, prenom, email, motDePasse, role: "ETUDIANT" }`
- `POST /api/auth/login`: `{ email, motDePasse }`
- `PUT /api/users/{matricule}/profile`: `{ currentPassword, nom, prenom }`
- `PUT /api/users/{matricule}/email`: `{ currentPassword, email }`
- `PUT /api/users/{matricule}/password`: `{ currentPassword, newPassword }`

Les changements de profil et d'email sont volontairement séparés. Le mot de passe actuel est obligatoire pour chaque modification.
