<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ExServ - Test REST</title>
    <style>
        body { margin: 0; font-family: Arial, sans-serif; background: #f1f3f5; color: #212529; }
        header { padding: 24px; color: white; background: #212529; text-align: center; }
        main { max-width: 950px; margin: 24px auto; padding: 0 16px; }
        .card { background: white; border-radius: 8px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 8px #0002; }
        label { display: block; margin-top: 10px; font-weight: bold; }
        input, select { width: 100%; box-sizing: border-box; padding: 9px; margin-top: 5px; }
        button { margin-top: 14px; padding: 10px 16px; border: 0; border-radius: 4px; color: white; background: #0d6efd; cursor: pointer; }
        button:hover { background: #0b5ed7; }
        pre { overflow: auto; padding: 14px; background: #212529; color: #f8f9fa; border-radius: 4px; min-height: 40px; }
    </style>
</head>
<body>
    <header><h1>ExServ - API REST</h1></header>
    <main>
        <section class="card">
            <h2>Utilisateurs</h2>
            <button type="button" onclick="loadUsers()">Charger les utilisateurs</button>
            <pre id="usersOutput"></pre>
        </section>

        <section class="card">
            <h2>Créer un utilisateur</h2>
            <form id="userForm">
                <label for="matricule">Matricule</label>
                <input id="matricule" required maxlength="10">
                <label for="nom">Nom</label>
                <input id="nom" required maxlength="30">
                <label for="prenom">Prénom</label>
                <input id="prenom" required maxlength="50">
                <label for="email">Email</label>
                <input id="email" type="email" required maxlength="254">
                <label for="motDePasse">Mot de passe</label>
                <input id="motDePasse" type="password" required maxlength="60">
                <label for="role">Rôle</label>
                <select id="role"><option>ETUDIANT</option><option>PROF</option><option>ADMIN</option></select>
                <button type="submit">Créer l'utilisateur</button>
            </form>
            <pre id="createOutput"></pre>
        </section>

        <section class="card">
            <h2>Messages publics</h2>
            <button type="button" onclick="loadMessages()">Charger les messages</button>
            <pre id="messagesOutput"></pre>
        </section>
    </main>

    <script>
        const API = '/ExServ/api';

        async function showResponse(response, target) {
            const data = await response.json().catch(() => ({ status: response.status }));
            document.getElementById(target).textContent = JSON.stringify(data, null, 2);
        }

        async function loadUsers() {
            const response = await fetch(`${API}/users`);
            await showResponse(response, 'usersOutput');
        }

        async function loadMessages() {
            const response = await fetch(`${API}/messages`);
            await showResponse(response, 'messagesOutput');
        }

        document.getElementById('userForm').addEventListener('submit', async (event) => {
            event.preventDefault();
            const user = {
                matricule: document.getElementById('matricule').value,
                nom: document.getElementById('nom').value,
                prenom: document.getElementById('prenom').value,
                email: document.getElementById('email').value,
                motDePasse: document.getElementById('motDePasse').value,
                role: document.getElementById('role').value,
                status: true
            };
            const response = await fetch(`${API}/users`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(user)
            });
            await showResponse(response, 'createOutput');
        });
    </script>
</body>
</html>
