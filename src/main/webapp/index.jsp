<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ExServ - API REST</title>
    <style>
        :root {
            font-family: Arial, Helvetica, sans-serif;
        }

        * {
            box-sizing: border-box;
        }

        body {
            margin: 0;
            color: #212529;
            background: #f1f3f5;
        }

        header {
            padding: 24px;
            color: #ffffff;
            text-align: center;
            background: #212529;
        }

        header h1 {
            margin: 0;
        }

        main {
            width: min(950px, calc(100% - 32px));
            margin: 24px auto;
        }

        .card {
            margin-bottom: 20px;
            padding: 20px;
            background: #ffffff;
            border-radius: 8px;
            box-shadow: 0 2px 8px rgb(0 0 0 / 12%);
        }

        label {
            display: block;
            margin-top: 10px;
            font-weight: bold;
        }

        input,
        select {
            width: 100%;
            margin-top: 5px;
            padding: 10px;
            border: 1px solid #ced4da;
            border-radius: 4px;
            font: inherit;
        }

        button {
            margin-top: 14px;
            padding: 10px 16px;
            border: 0;
            border-radius: 4px;
            color: #ffffff;
            background: #0d6efd;
            cursor: pointer;
            font: inherit;
        }

        button:hover {
            background: #0b5ed7;
        }

        button:disabled {
            background: #6c757d;
            cursor: wait;
        }

        pre {
            min-height: 40px;
            overflow: auto;
            margin-top: 16px;
            padding: 14px;
            color: #f8f9fa;
            background: #212529;
            border-radius: 4px;
            white-space: pre-wrap;
            word-break: break-word;
        }

        pre.error {
            color: #842029;
            background: #f8d7da;
        }
    </style>
</head>
<body>
    <header>
        <h1>ExServ - API REST</h1>
    </header>

    <main>
        <section class="card">
            <h2>Utilisateurs</h2>
            <button id="loadUsersButton" type="button">
                Charger les utilisateurs
            </button>
            <pre id="usersOutput">Aucune donnée chargée.</pre>
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
                <select id="role">
                    <option value="ETUDIANT">ETUDIANT</option>
                    <option value="PROF">PROF</option>
                    <option value="ADMIN">ADMIN</option>
                </select>

                <button id="createUserButton" type="submit">
                    Créer l'utilisateur
                </button>
            </form>

            <pre id="createOutput">Aucune donnée envoyée.</pre>
        </section>

        <section class="card">
            <h2>Messages publics</h2>
            <button id="loadMessagesButton" type="button">
                Charger les messages
            </button>
            <pre id="messagesOutput">Aucune donnée chargée.</pre>
        </section>
    </main>

    <script>
        // Le contexte est fourni par Tomcat, par exemple : /ExServ.
        // Cela évite de coder le nom de l'application en dur.
        const API = '${pageContext.request.contextPath}/api';

        function afficherResultat(elementId, resultat) {
            const element = document.getElementById(elementId);
            element.textContent = JSON.stringify(resultat, null, 2);
            element.classList.toggle('error', !resultat.success);
        }

        async function lireReponse(response) {
            const contentType = response.headers.get('content-type') || '';
            let body;

            if (contentType.includes('application/json')) {
                body = await response.json();
            } else {
                const text = await response.text();
                body = text || null;
            }

            return {
                status: response.status,
                success: response.ok,
                body: body
            };
        }

        async function appelerApi(url, options = {}) {
            try {
                const response = await fetch(url, options);
                return await lireReponse(response);
            } catch (error) {
                return {
                    status: 0,
                    success: false,
                    body: {
                        message: 'Impossible de contacter l\'API.',
                        detail: error.message
                    }
                };
            }
        }

        async function chargerUtilisateurs() {
            const button = document.getElementById('loadUsersButton');
            button.disabled = true;

            const resultat = await appelerApi(`${API}/users`);
            afficherResultat('usersOutput', resultat);

            button.disabled = false;
        }

        async function chargerMessages() {
            const button = document.getElementById('loadMessagesButton');
            button.disabled = true;

            const resultat = await appelerApi(`${API}/messages`);
            afficherResultat('messagesOutput', resultat);

            button.disabled = false;
        }

        document.getElementById('loadUsersButton')
            .addEventListener('click', chargerUtilisateurs);

        document.getElementById('loadMessagesButton')
            .addEventListener('click', chargerMessages);

        document.getElementById('userForm')
            .addEventListener('submit', async function (event) {
                event.preventDefault();

                const button = document.getElementById('createUserButton');
                button.disabled = true;

                const user = {
                    matricule: document.getElementById('matricule').value.trim(),
                    nom: document.getElementById('nom').value.trim(),
                    prenom: document.getElementById('prenom').value.trim(),
                    email: document.getElementById('email').value.trim(),
                    motDePasse: document.getElementById('motDePasse').value,
                    role: document.getElementById('role').value,
                    status: true
                };

                const resultat = await appelerApi(`${API}/users`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify(user)
                });

                afficherResultat('createOutput', resultat);

                if (resultat.success) {
                    document.getElementById('userForm').reset();
                }

                button.disabled = false;
            });
    </script>
</body>
</html>
