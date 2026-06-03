# AlgoCraft Demo World

This repository contains a fully fledged Minecraft demo world built on top of the [AlgoCraft](https://github.com/MCAlgoVisualizations-Bsc/AlgoCraft) library/framework.

It runs as a Minestom-based server and showcases algorithms directly inside the world, using custom layouts, scenes, events, and UI elements to turn the experience into an interactive visualization playground.

## What Is Included

The demo currently registers a curated set of algorithm visualizations, including:

- Graph search with Breadth-First Search and Depth-First Search
- Sorting visualizations such as Selection Sort, Insertion Sort, and Exchange Sort
- Maze and pathfinding visualizations such as A*, BFS, DFS, and Greedy Best-First Search

Each algorithm is presented with its own scene, presentation text, and world layout so players can watch the execution step by step.

## How It Works

When the server starts, players spawn into the demo world in Adventure mode. The world is configured as a visual demo space, and players receive a Nether Star prompt that can be used to choose an algorithm to visualize.

The project uses AlgoCraft to:

- register algorithm instances
- bind algorithm-specific scenes and layouts
- react to algorithm events such as comparisons, swaps, movement, and path discovery
- present each algorithm with tailored UI and contextual text

## Commands

The server includes a small set of Minestom commands that are useful while exploring the demo:

- `/greet` - sends a simple greeting message.
- `/tp` - teleports to a player or coordinates, depending on the arguments used.
- `/gamemode` or `/gm` - changes your game mode.
- `/spawn` - returns a player to the AlgoCraft demo layout.
- `/invite <player>` - invites another player into your visualization instance.
- `/accept` - lists pending invites.
- `/accept <player>` - accepts a specific invite and joins that player.
- `/pendingInvites` - shows the invites waiting for you.

## UI And Interaction

The UI is designed around direct in-world interaction rather than a separate menu screen.

- Players start in Adventure mode with flight enabled, which keeps the focus on exploration and visualization.
- The demo world uses custom layouts for different algorithm families, including graph search, sorting, and maze/pathfinding.
- Algorithm presentations include short descriptions and icons so each visualization is easy to recognize.
- Some algorithms support point-of-view style interaction, letting the player follow the execution from inside the scene.

## Running The Demo

This project uses Gradle and targets Java 25.

```bash
./gradlew run
```

On Windows, use:

```powershell
gradlew.bat run
```

Then connect to the local server with your Minecraft client.

## Notes

The main entry point is [demo/src/main/java/io/github/mcalgovisualizations/demo/Main.java](demo/src/main/java/io/github/mcalgovisualizations/demo/Main.java), and the algorithm registrations live in [demo/src/main/java/io/github/mcalgovisualizations/demo/RegisterAlgo.java](demo/src/main/java/io/github/mcalgovisualizations/demo/RegisterAlgo.java).