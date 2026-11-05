# WMS Cícero — app Android

APK que abre o WMS (https://inventario-cicero.netlify.app) em tela cheia nos coletores (Android 8+).
O conteúdo vem do site: atualização do WMS não precisa de APK novo.

- Compila sozinho no GitHub Actions a cada push na `main` (aba Actions → artefato `wms-cicero-apk`).
- Assinatura: segredos `KEYSTORE_B64` e `KEYSTORE_PASS` do repositório. **Guarde a chave**: sem ela não dá para instalar uma versão nova por cima.
