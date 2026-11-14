# MusicFlow

A simple desktop music player created for the Software Design Patterns final group project.

## Technologies

- Java 17
- Maven
- Java Swing
- Java Sound API
- JUnit 5

## Features

- Import and play local WAV files
- Play, pause, stop, next and previous
- Create playlists and add tracks
- Sequential and shuffle playback
- Volume control and echo effect
- Synchronized lyrics
- Track count and total duration statistics

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|----------------|---------|
| Facade | MusicFacade | Provides a simple interface to the application subsystems |
| Builder | PlaylistBuilder | Creates playlists step by step |
| Observer | PlaybackService, PlaybackListener, LyricsPanel, NowPlayingPanel | Updates track information, progress and lyrics |
| Strategy | PlaybackOrderStrategy, SequentialStrategy, ShuffleStrategy | Changes the playback order |
| Decorator | AudioStream, WavAudioStream, AudioDecorator, VolumeDecorator, EchoDecorator | Adds audio effects to the WAV stream |
| Visitor | LibraryVisitor, DurationVisitor, TrackCountVisitor | Calculates track and playlist statistics |

## Team Responsibilities

| Member | Patterns |
|--------|----------|
| Student 1 | Facade and Builder |
| Student 2 | Observer and Strategy |
| Student 3 | Decorator and Visitor |

Shared models, GUI and integration are developed collaboratively.

## Requirements

- JDK 17
- Maven
- An available audio output device

## Run

Open a terminal in the project directory containing `pom.xml`.

Build the project and run the tests:

```bash
mvn clean package
```

Start the application:

```bash
java -jar target/musicflow-1.0.0.jar
```

Alternatively, open the project in IntelliJ IDEA, select JDK 17, load the Maven project and run:

```text
src/main/java/com/musicflow/Main.java
```

## Usage

1. Select a demonstration track or click **Import WAV**.
2. Select a track and click **Play**, or double-click it.
3. Use **Pause**, **Stop**, **Next** and **Previous** to control playback.
4. Enter a playlist name and click **Create**.
5. Select tracks in the library and click **Add selected library tracks**.
6. Select a track inside the playlist to play from that playlist.
7. Choose **Sequential** or **Shuffle**.
8. Adjust **Volume** or enable **Echo**.

## Synchronized Lyrics

Place a WAV file and an LRC file with the same name in the same directory before importing:

```text
My Song.wav
My Song.lrc
```

Save the LRC file as UTF-8. Each line contains a timestamp and text:

```text
[00:00.00]Introduction
[00:05.20]First line
[00:10.50]Second line
```

Import the WAV file. The application automatically loads the matching LRC file and highlights lines during playback.

If you change the LRC file after importing, restart the application and import the WAV again.

## Demonstration Audio

Three generated instrumental samples are included:

- morning.wav
- evening.wav
- starlight.wav

Each sample lasts 15 seconds. Their demonstration lyric lines are defined in `Main.java`.

## Tests

Run the JUnit tests:

```bash
mvn test
```

Tests cover all six patterns, audio sample processing, playback events and error handling. Audio tests do not require a physical audio device.

## Limitations

- Local WAV playback only; MP3 and online streaming are not supported.
- Audio is converted to 44.1 kHz, 16-bit, mono PCM when supported.
- Imported tracks and playlists remain in memory only during the current session.
