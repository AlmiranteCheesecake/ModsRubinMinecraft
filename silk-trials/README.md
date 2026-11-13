# Silk Trials

Mod Fabric para Minecraft 26.3.

- **Trial spawner**: quebrado com Toque Suave, dropa o bloco com o mob dentro. Recolocado, volta com o mesmo mob e sem cooldown.
- **Vault**: quebrado com Toque Suave, dropa o bloco com o loot e a chave que usa. Recolocado, não lembra quem já abriu.
- Blocos ominous voltam ominous.
- O nome do item mostra o que ele guarda: o mob do spawner ("Trial Spawner (Breeze)") e a chave que abre o vault ("Vault (Ominous Trial Key)"), no idioma do jogo.
- Os dois blocos passam a ser minerados com picareta: 3 segundos com uma de diamante, menos com Eficiência (0,7 s com Eficiência V). No jogo normal são 75 segundos com qualquer ferramenta.
- Sem Toque Suave nada muda: o bloco quebra e não dropa nada.

Precisa de Fabric API.

## Compilar e testar

```
gradlew build         # gera build/libs/silk_trials-<versão>.jar
gradlew runGameTest   # roda os testes dentro de um servidor sem tela
```
