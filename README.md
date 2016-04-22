#Catnip
One of our cats REALLY likes to chase the mouse cursor on the screen. It can be very hard to use the computer with a
cat in front of the screen so I made this game for her to play, preferably on a screen other than the one I'm using.

## Play
If you want to try it out go [here](http://wadegulbrandsen.github.io/catnip/public/index.html).

## Installation instructions
Copy the contents of public to a web server and then navigate to index.html in a web browser

## Usage
* After the page loads you can a change the sliders to control the number of birds on the screen at a time and the speed
that they move
* To start the program click the Start button at the bottom of the options window
* To play/pause the animation click the button in the left of the toolbar
* To show or hide the options click the button in the left of the toolbar
* To enter/exit fullscreen mode click the button in the right of the toolbar

## Todo List

- [ ] Multiple backgrounds and a way to select them
- [ ] Add a start page with instructions
- [x] Merge the show/hide options button into a single component
- [ ] Make the loading page prettier
- [x] Animated loading image
- [x] Button for fullscreen mode
- [x] Button to play/pause the animation
- [x] Make the toolbar and the buttons in it pretty
- [ ] Add a countdown to start the program and hide the options
- [ ] Create functions to start/stop/pause the animation
- [ ] Create functions to show/hide the options
- [x] Move the sprite handling to its own namespace
- [ ] Have the birds change direction once in a while
- [ ] Have different paths that the birds can take while moving
- [ ] Sound effects
- [ ] Add interaction with the birds so that cats can whack them on a touchscreen

### Version History

#### Version 0.2.1 - April 19, 2016
- Moved the sprite handling to a separate namespace
  - This is using the DOM instead of a canvas so that it fits in better with React.js/Reagent
- Put all the possible sprite components into an atom on load so they don't need to be calculated each frame

#### Version 0.2.0 - April 9, 2016
- Created a toolbar
- Replaced the text based show/hide options button with a toolbar
- Added a proper full screen mode instead of just having text saying to press F11
- Added a spinner to the loading screen
- Added an icon to the options window
- Added a start/stop button to the options window
- Added a close button to the options window
- Fixed a bug in the slider that was causing them to be stuck on the left and could only make the values higher

#### Version 0.1.1 - April 6, 2016
Refactoring the code to make the source more readable

#### Version 0.1.0 - April 4, 2016 (initial release)
Game is working.