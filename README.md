# Paper Manager (minimal README)

Spring Boot (Java 21) + React/Vite (TypeScript) の論文管理アプリ。
バックエンドとフロントエンドを同一リポジトリ（モノレポ）で運用します。

## 構成
```
paper-app/
├─ backend/   # Spring Boot (Gradle)
└─ frontend/  # Vite + React + TS
```

## 必要ツール
- Java 21
- Node.js 18+
- Git

## 起動手順（開発）
### Backend（H2: 永続ファイルDB）
```powershell
cd backend
./gradlew bootRun   # Windows は .\gradlew でもOK
```
- H2 Console: http://localhost:8080/h2-console
- Swagger UI: http://localhost:8080/swagger-ui/index.html

### Frontend（Vite）
```powershell
cd frontend
npm i
npm run dev
```
- Dev URL: http://localhost:5173

> メモ: 開発時は Vite の proxy で `/api` と `/v3` を `http://localhost:8080` に向けると CORS を回避できます。
