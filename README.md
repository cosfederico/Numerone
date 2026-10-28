<p align="center">
  <img src="docs/icon.png" alt="Numerone app icon: an hourglass whose sand is made of digits" width="128" height="128">
</p>

<p align="center">
  <img src="docs/title.svg" alt="Numerone" width="420">
</p>

<p align="center">
  <b>An Android app for practicing number memorization.</b><br>
  <sub>VIVI OGNI GIORNO COME SE FOSSE IL PRIMO</sub>
</p>

<p align="center">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3ddc84?style=flat-square&logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-7f52ff?style=flat-square&logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose and Material 3" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285f4?style=flat-square&logo=jetpackcompose&logoColor=white">
  <img alt="Works offline, no permissions" src="https://img.shields.io/badge/works-offline-c9a646?style=flat-square">
  <img alt="English and Italian" src="https://img.shields.io/badge/EN%20%C2%B7%20IT-bilingual-2a2623?style=flat-square">
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#get-started">Get started</a> ·
  <a href="#license">License</a> ·
  <a href="#credits">Credits</a>
</p>

<br>

<table align="center">
  <tr>
    <!-- <td align="center"><img src="docs/practice-idle.png" alt="Practice screen, ready to start" width="230"></td> -->
    <td align="center"><img src="docs/practice-memorize.png" alt="A twenty-digit number on screen with the timer running" width="230"></td>
    <td align="center"><img src="docs/goal.png" alt="Goal screen with a target time, the record and reached goals" width="230"></td>
    <td align="center"><img src="docs/results.png" alt="Results screen with a time chart and the history of runs" width="230"></td>
  </tr>
  <tr>
    <!-- <td align="center">Start a run</td> -->
    <td align="center">Memorize the Numerone</td>
    <td align="center">Set your goal</td>
    <td align="center">Monitor your progress</td>
  </tr>
</table>

<br>

Memorizing a long number sounds like a party trick, until you learn how to do it, and it becomes a party trick but that you never believed possible! Practice the numerone: the app gives you the 20-digit number, it handles timing you and checking your answer. If your answer it's correct, it saves your score, and tracks it over time.

<br>

<table>
  <tr>
    <td width="25%" valign="top">
      <h3>🔢 Roll your luck</h3>
      The number is generated so pairs do not repeat, making sure you can memorize it correctly.
    </td>
    <td width="25%" valign="top">
      <h3>⏱️ Track your time</h3>
      The app tracks the time of each perfect answer, but you can also track how many mistakes you make.
    </td>
    <td width="25%" valign="top">
      <h3>🏆 Set your goals </h3>
      Set a goal, and track your goals over time. Periodic notifications remind you to practice!
    </td>
    <td width="25%" valign="top">
      <h3>🔒 Fully portable</h3>
      The app requires no account, no permissions and no internet. It stays fully local, and you can reset your times whenever you want.
    </td>
  </tr>
</table>

<br>

## Features

<table>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/feature-check.png" alt="The answer checked against the original number, with three wrong digits in red and a Too bad message" width="100%">
      <br>
      <sub></sub>
      <h3>Check digits</h3>
      Your answer is lined up under the original number and checked digit by digit, revealed one at a time. Right digits are green, wrong ones red, and the app counts the mistakes for you and tracks them.
    </td>
    <td width="50%" valign="top">
      <img src="docs/feature-celebration.png" alt="The gold goal-reached screen with a trophy and confetti" width="100%">
      <br>
      <sub></sub>
      <h3>Goal celebration</h3>
      When you reach your goal, the app plays a celebration! A trophy, confetti and a round of applause follow, and the goal moves to your shelf of reached goals.
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/feature-goal-dialog.png" alt="The goal dialog with a time in seconds and an encouraging motto" width="100%">
      <br>
      <sub></sub>
      <h3>Set your goal</h3>
      Input the goal you want to reach. Each time you open the dialog, a new motto pushes you to aim higher.
    </td>
    <td width="50%" valign="top">
      <img src="docs/feature-errors.png" alt="The errors chart for a week, with green dots for perfect runs and red dots for runs with mistakes" width="100%">
      <br>
      <sub></sub>
      <h3>Mistakes tracking</h3>
      Switch the hart from Time to Errors to see how many digits you missed in each run over time. Green dots are perfect runs. Pick any period from today to a year, or the whole history.
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <img src="docs/feature-filters.png" alt="The Outcome filter menu with All, Correct and Wrong" width="100%">
      <br>
      <sub></sub>
      <h3>Filter your results</h3>
      Narrow the history by date range, by outcome (correct or wrong) or by runs under or over a time. The list counts the results as you filter.
    </td>
    <td width="50%" valign="top">
      <img src="docs/feature-settings.png" alt="The Settings screen with a name field and an English and Italian language switch" width="100%">
      <br>
      <sub></sub>
      <h3>Input your name</h3>
      Numerone greets you by name, up to 20 letters, so it feels more personal. Switch between English and Italian whenever you like. Settings also has an About page and a button that deletes all your results.
    </td>
  </tr>
</table>

<br>

## Get started

You need Android Studio, or a recent JDK with the Android SDK, and a device or emulator running Android 8.0 or later.

```bash
git clone <this-repo>
cd Numerone
./gradlew installDebug   # build and install on a connected device or emulator
./gradlew test           # unit tests for the number logic
```

On Windows, use `gradlew.bat`. If Android Studio hasn't done it for you, point `local.properties` at your SDK:

```properties
sdk.dir=/path/to/Android/Sdk
```

The code lives in `app/src/main/java/it/numerone`:

| File | Contents |
| --- | --- |
| `Numerone.kt` | Number generation, time formatting, goal logic |
| `Practice.kt` | The run: memorize, recall, check, celebrate |
| `Goal.kt` | Target time, record, reached goals |
| `Results.kt` | Charts, filters and history |
| `Settings.kt` | Name, language, data deletion, About |
| `Strings.kt` | All texts, in Italian and English |
| `MainActivity.kt` | Theme, storage and tab navigation |

The launcher icon comes from `tools/make_icon.py`, which draws the hourglass from the app's own serif digits (it needs `fontTools`).

<br>

## License

*Numerone* is licensed under MIT. Copyright (c) 2026 Federico Coscia

## Credits

The app draws on the memorization course and techniques of [Vincenzo Palmisano](https://vincenzopalmisano.net) and 6inTempo S.p.A. For the method itself and everything beyond this small practice tool, visit [vincenzopalmisano.net](https://vincenzopalmisano.net).

*Numerone* is an independent project by enthusiasts of the course and of memorization techniques. It is an open-source project and is not affiliated with, sponsored by or endorsed by Vincenzo Palmisano or 6inTempo S.p.A. For more information about them and their memorization techniques, please visit [vincenzopalmisano.net](https://vincenzopalmisano.net).
