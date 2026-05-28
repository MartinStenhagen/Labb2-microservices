# Lokala secrets for Docker Compose

Den har mappen innehaller bara instruktioner och exempel. Riktiga secret-filer ska inte commitas.

Skapa filerna innan du kor `docker compose up`:

```powershell
New-Item -ItemType Directory -Force secrets

[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) |
  Set-Content -NoNewline secrets\mysql-root-password.txt

"labb2_rabbit" |
  Set-Content -NoNewline secrets\rabbitmq-username.txt

[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(24)) |
  Set-Content -NoNewline secrets\rabbitmq-password.txt

[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) |
  Set-Content -NoNewline secrets\internal-api-key.txt

[Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32)) |
  Set-Content -NoNewline secrets\auth-oauth-client-secret.txt
```

Om du vill anvanda AI-svar lokalt kan du antingen satta `OPENROUTER_API_KEY` i din shell eller lagga den i en `.env`-fil. `.env` ignoreras av git.

