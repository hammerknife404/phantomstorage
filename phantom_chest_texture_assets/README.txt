Phantom Chest texture assets

Namespace used: phantomchest

PNG files:
- assets/phantomchest/textures/entity/phantom_chest.png
- assets/phantomchest/textures/gui/container/phantom_chest.png
- assets/phantomchest/textures/item/phantom_chest_summoner.png
- assets/phantomchest/textures/block/phantom_chest_preview.png

Notes:
- These are original vanilla-style textures, not copied Mojang files.
- Chest size of 1.5x normal should be handled in your entity/model renderer scale, not by increasing the PNG dimensions.
- GUI is laid out for a 54-slot container: 6 rows x 9 columns.
- Entity texture is 64x64 RGBA and semi-transparent. Depending on your renderer, you may need to map UVs to your custom chest model.
