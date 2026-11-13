# ModsRubinMinecraft

Mods de Minecraft do Rubin. Cada mod fica na sua própria pasta e é um projeto Gradle independente.

| Pasta | Mod | Minecraft | Loader |
|-------|-----|-----------|--------|
| [`silk-trials`](silk-trials) | Trial spawners e vaults dropam com Toque Suave e funcionam ao serem recolocados | 26.3 | Fabric |

## Compilar um mod

Precisa de um JDK 25. Dentro da pasta do mod:

```
gradlew build
```

O `.jar` sai em `build/libs`.
