Here is the translation of the provided document into English, formatted in Markdown:

***

<div align="center">

# Amirkabir University of Technology
### (Tehran Polytechnic)

<br>

## Final Project for Mobile Device Programming Course
### Music Streaming and Social Network Application

<br>

**Course Professor:**
Dr. Masoomeh Taremi Rad

<br>

**Computer Engineering Department**
**Spring 2026 (1405)**

</div>

---

## Table of Contents

1. Introduction ...................................................................................................................... 1
2. Basic Rules and Development Standards (Clean Code & System Design) ........................................ 1
3. Architecture & Tech Stack ................................................................................................ 2
4. Server-side and Mock Data (Backend & Mock Data) ................................................................. 2
5. UI Details and Animations (UI/UX in Compose) ...................................................................... 3
    5.1 First Tab: Home ......................................................................................................... 3
    5.2 Second Tab: Search ................................................................................................... 4
    5.3 Third Tab: Downloads ............................................................................................... 4
    5.4 Fourth Tab: Playlists ................................................................................................. 4
    5.5 Fifth Tab: My Profile ................................................................................................. 4
6. Professional Media Player (Media Playback) ............................................................................ 5
7. Business Logic .................................................................................................................. 6
8. Social Network and Chat System (Real-time Chat) ................................................................... 6
9. Local Data Management .................................................................................................... 7
10. Secondary Pages, Creativity, and Developer Autonomy .......................................................... 7
11. Expected Output ............................................................................................................. 8

---

## 1. Introduction
Your final project is to develop a music streaming platform (similar to Spotify or Melodify) combined with the features of a small social network. You can choose any creative name you like for your application.

In this project, you will go through the entire software development life cycle, from designing the database and custom backend to implementing Android concepts (Jetpack Compose, Clean Architecture, UDF, Coroutines/Flow), and you will implement background media playback. Our goal in this project is to evaluate your ability to write Clean Code, implement complex animations, and efficiently manage states (State Management).

## 2. Basic Rules and Development Standards (Clean Code & System Design)
Before diving into the features, adhering to the following standards in all parts of the code is mandatory and will directly impact your grade:

*   **Design System:** Using Hardcode values for colors, font sizes, and Padding/Margin is strictly prohibited. You must define a custom `MaterialTheme` including Typography, Colors, and Shapes.
*   **Clean Code and String Resources:** No text should be written directly in the code. All texts must be called via the `strings.xml` file.
*   **Localization (Two-Language Support):** The application must fully support two languages (Persian and English). The layout must automatically switch between RTL (Right-to-Left for Persian) and LTR (Left-to-Right for English) upon language change.
*   **Dark/Light Mode:** The application must have both themes and should switch either via system settings or from within the app itself. The color palette must be optimized for both modes to maintain text readability.
*   **Custom Icon:** Using the default Android icon is not accepted. The application must feature a custom Adaptive Icon.

## 3. Architecture & Tech Stack
The project must be implemented with strict adherence to Clean Architecture principles and appropriate layering (UI, Domain, Data).

*   **UI Design Pattern:** Use MVI or MVVM with a UDF (Unidirectional Data Flow) approach. All states of each page must be aggregated into a Data Class named `UiState` and passed to Compose via `StateFlow`.
*   **Event Management:** User interactions must be sent to the ViewModel as an Intent or Event. One-time events (like showing a network error Snackbar) must be managed using `SharedFlow` or `Channel`.
*   **Dependency Injection (DI):** Using Hilt or Koin to manage the application's dependencies is mandatory (Repositories, UseCases, Database, and Network Client must be injected).
*   **Data Stream Management in Lists (Paging 3):** For all long lists (search, playlist songs, and chat history), using the Paging 3 library is mandatory. Loading all data at once into a `LazyColumn` is not accepted.
*   **I/O Management and Thread Safety:** No network or database operations should run on the Main Thread. Proper use of `Dispatchers.IO` and Coroutines is essential.

## 4. Server-side and Mock Data (Backend & Mock Data)
Implementing the backend is your responsibility. You can use BaaS (Backend as a Service) platforms like Firebase, Supabase, or write custom code with Ktor/Node.js.

*   **Mandatory Content:** Your database must include at least 50 real songs (you may use instrumental tracks, short clips, or any other source).
*   **Metadata:** Each song must have at least the following fields:
    *   `id`
    *   `title`
    *   `artist_name`
    *   `cover_image_url`
    *   `audio_url`

## 5. UI Details and Animations (UI/UX in Compose)
The user interface should be modern, smooth, and full of visual details. The use of Fluid animations, Scale effects when clicking buttons, and smooth Transitions throughout the app is expected. The general expected features across pages are as follows:

*   **Top Bar:** On all main pages, the logo and app name should be on the right, while the user's profile picture, bell icon (notifications), and gear icon (settings) should be on the left.
*   **Shimmer Effect:** Until data is loaded from the server, all list sections and cards must have a Shimmer loading animation (skeleton loading).
*   **Shared Element Transitions:** Moving between the mini-player at the bottom of the screen and opening the full player screen must be implemented using Shared Transition animations, so the song cover smoothly enlarges and centers on the screen.

### 5.1 First Tab: Home
This page is the showcase of your application and consists of the following sections:
*   **Daily Suggestions Slider (Carousel):** At the top of the page, place an automatic horizontal slider that displays new albums, special daily picks, or trending songs with large images.
*   **Quick Actions:** Four buttons that include the following:
    *   Liked Songs
    *   Recently Played
    *   My Playlists
    *   Top Artists
*   **Sliders (LazyRow):** Rows for "Most Popular", "Newest", "Global Playlists", and "Local Playlists".

### 5.2 Second Tab: Search
*   **Live and Smart Search (Debounce):** A request should not be sent to the server with every keystroke. You must create an appropriate delay using the `debounce` operator in Flow.
*   **Filter Chips:** Buttons for filtering results (Song, Artist, etc.).
*   **History:** Display previous search history (saved in Room) with the ability to delete entries.

### 5.3 Third Tab: Downloads
Includes a list of downloaded songs for offline listening, which also has sorting capabilities and deletion via Swipe to Dismiss.

### 5.4 Fourth Tab: Playlists
*   Use `LazyVerticalGrid` to display two-column colored cards.
*   Categorized into "Global Music", "Local Music", and "User Playlists".

### 5.5 Fifth Tab: My Profile
*   **Avatar:** User avatar image with the ability to change it.
*   **Premium Status:** Display a golden indicator (Badge) showing premium subscription status.
*   **Renew/Buy Subscription Button:** Clicking this button runs a mock process that sets the user's Premium variable to `true`.
*   **Settings:** Access to application settings (changing language and theme).

## 6. Professional Media Player (Media Playback)
Implementing this section carries the most technical weight in the project.

*   **ExoPlayer and MediaSession:** Music playback operations must be performed as a Background Service. If the user exits the app, the music should not stop.
*   **Audio Focus:** Your player must be smart. Upon receiving an incoming call or playing system sounds (like a Telegram voice message), the song must pause or lower in volume (Duck), and resume playing afterward.
*   **Smart Cache (ExoPlayer CacheDataSource):** When a user streams a song, its file must simultaneously be saved in the ExoPlayer cache so that in case of a Replay or seeking backward/forward, internet data is not consumed again.
*   **Crossfade:** Fading out the audio of the song in its final seconds and blending it with the initial seconds of the next song.
*   **Notification Controls:** Display playback status in the system notification bar and lock screen, along with Play/Pause/Next buttons.
*   **Mini Player:** A small bar that always floats above the Bottom Navigation, showing the currently playing song.
*   **Now Playing Screen UI:**
    *   **Animated Cover:** In the center of the screen, place the song cover in the shape of a disk (CD) that rotates while the song is playing and stops when the song is paused.
    *   **Dynamic Color Scheme (Palette API):** The player screen's background color should not be static. The dominant color of the song cover must be extracted and applied as a smooth gradient on the background.
    *   **Audio Visualizer (Drawn with Canvas):** Design an animated equalizer or Waveform on the player screen that moves while the song plays. (Using Lottie or GIF is prohibited; it must absolutely be drawn with `Canvas` in Compose).
    *   **Sleep Timer:** Implement a feature where the user can set a time (e.g., 15 minutes from now), issuing an automatic pause command using Coroutines.
    *   **Playback Speed Control:** Ability to change the playback speed to 1.5x or 2x via ExoPlayer controllers.

## 7. Business Logic
*   **Regular vs. Premium User (Premium Logic):** Regular users can listen to all songs fully and unlimitedly with no interruptions. The only difference lies in the offline download capability. The download button is only active for Premium users, and if a regular user clicks it, they should see a message prompting them to upgrade their account.
*   **Managing Downloads with WorkManager:** When a premium user downloads a song, this process must be handed over to `WorkManager` to be executed in the background.
*   **Smart Playback:** The Repository layer must check if the user presses Play on a song and its offline file is already downloaded; instead of consuming internet bandwidth, the music should play directly from the local file.

## 8. Social Network and Chat System (Real-time Chat)
This section is the primary criterion for evaluating your ability to manage sockets and data streams:

*   **Follow Users and Public Playlists:** Users can search for other users' profiles (friends) and follow them. Users can also see and play their friends' public playlists.
*   **WebSocket Connection:** The Direct Message (DM) section must be completely Real-time, and the list of new messages must update live. Using Polling is entirely unacceptable.
*   **Read/Delivered Receipts:** Sent messages must have statuses for "sending" (clock), "sent" (single tick), and "read" (double tick).
*   **Typing Indicator:** Display the phrase "...is typing" when the other user is typing.
*   **Song Sharing:** The user can send a song to a friend's direct message via the Share button. The message containing the song must have a custom UI (Mini Card) that, when clicked, plays directly in the player.
*   **Offline Chat Management:** Received messages must be saved in the Room database so that in case of an internet disconnection, the user can read the history.

## 9. Local Data Management
*   **Room Database:** Search history, liked songs list, downloaded file paths, and offline message history must all be saved in the device's database.
*   **DataStore Preferences:** Application settings such as dark mode, font size, selected language, and user status (regular/premium) must be managed with DataStore.

## 10. Secondary Pages, Creativity, and Developer Autonomy
In this project's documentation, the core page structure, system architecture, and critical workflows (Flows) have been detailed. However, not every micro-interaction and secondary page is dictated word-for-word to the programmer.

You are expected to design and implement other secondary pages and complementary sections of the application relying on your own understanding of User Experience (UX) and maintaining the consistency of the application's Design System. Some of these pages and sections include:

*   **Liked / Recently Played Songs Page:** It is expected that these pages are not just a simple list, but feature an attractive header, a "Play All/Shuffle" button, and the ability to quickly remove items from the list (e.g., with a Swipe to Dismiss animation).
*   **Followed Artists and Users List:** A clean user interface (e.g., grid or list) to manage people the user has followed, along with an Unfollow button.
*   **Settings Page:** A standard page for changing the app language (dynamic LTR/RTL change), changing the theme (dark/light/system), and logging out of the account.
*   **Empty States:** Designing and placing appropriate images or animations for when a playlist is empty, a search yields no results, or the internet is disconnected.

## 11. Expected Output
*   The exported application file (no need to upload the project source code).
*   A short video demonstrating the application's functionality.