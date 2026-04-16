# Postman collections for inventory_backend

## Cómo importar
1. `git pull`
2. En Postman: Import -> seleccionar `postman/collections/inventory_backend_collection_v1.0.json`.
3. Importar environment: `postman/environments/local.json` y activar el environment `local`.

## Actualizar colección
1. Editar en Postman.
2. Exportar Collection v2.1.
3. Reemplazar archivo en `postman/collections/`.
4. `git add postman/collections/... && git commit -m "postman: update collection vX.Y" && git push`

## Convenciones
- **Nombres**: `{{base_url}}` para host; collection file `inventory_backend_collection_vX.Y.json`.
- **Versionado**: usar tags o releases para snapshots estables.
