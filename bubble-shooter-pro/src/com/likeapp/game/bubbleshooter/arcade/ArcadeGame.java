/*
 *                 [[ Frozen-Bubble ]]
 *
 * Copyright (c) 2000-2003 Guillaume Cottenceau.
 * Java sourcecode - Copyright (c) 2003 Glenn Sanson.
 *
 * This code is distributed under the GNU General Public License
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 *
 * Artwork:
 *    Alexis Younes <73lab at free.fr>
 *      (everything but the bubbles)
 *    Amaury Amblard-Ladurantie <amaury at linuxfr.org>
 *      (the bubbles)
 *
 * Soundtrack:
 *    Matthias Le Bidan <matthias.le_bidan at caramail.com>
 *      (the three musics and all the sound effects)
 *
 * Design & Programming:
 *    Guillaume Cottenceau <guillaume.cottenceau at free.fr>
 *      (design and manage the project, whole Perl sourcecode)
 *
 * Java version:
 *    Glenn Sanson <glenn.sanson at free.fr>
 *      (whole Java sourcecode, including JIGA classes
 *             http://glenn.sanson.free.fr/jiga/)
 *
 * Android port:
 *    Pawel Aleksander Fedorynski <pfedor@fuw.edu.pl>
 *    Copyright (c) Google Inc.
 *
 *          [[ http://glenn.sanson.free.fr/fb/ ]]
 *          [[ http://www.frozen-bubble.org/   ]]
 *          
 * Bubble-Shooter-Pro Project:http://code.google.com/p/bubble-shoot/
 */

package com.likeapp.game.bubbleshooter.arcade;

import java.util.Random;
import java.util.Vector;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.likeapp.game.bubbleshooter.BmpWrap;
import com.likeapp.game.bubbleshooter.BubbleArcadeActivity;
import com.likeapp.game.bubbleshooter.BubbleManager;
import com.likeapp.game.bubbleshooter.GameScreen;
import com.likeapp.game.bubbleshooter.ImageSprite;
import com.likeapp.game.bubbleshooter.LaunchBubbleSprite;
import com.likeapp.game.bubbleshooter.PenguinSprite;
import com.likeapp.game.bubbleshooter.SoundManager;
import com.likeapp.game.bubbleshooter.Sprite;

public class ArcadeGame extends GameScreen {
	private static final int MIN_LINE_Y = 360;
	public final static int HORIZONTAL_MOVE = 0;
	public final static int FIRE = 1;

	public final static int KEY_UP = 38;
	public final static int KEY_LEFT = 37;
	public final static int KEY_RIGHT = 39;
	public final static int KEY_SHIFT = 16;

	public static String PARAMETER_PLAYER = "player";
	public static String PARAMETER_OFFLINE = "offline";

	// Change mode (normal/colorblind)
	public final static int KEY_M = 77;
	// Toggle sound on/off
	public final static int KEY_S = 83;

	boolean modeKeyPressed, soundKeyPressed;

	boolean levelCompleted = false;

	private int levelRows;

	BmpWrap background;
	BmpWrap[] bubbles;
	BmpWrap[] bubblesBlind;
	BmpWrap[] frozenBubbles;
	BmpWrap[] targetedBubbles;
	Random random;

	LaunchBubbleSprite launchBubble;
	double launchBubblePosition;

	PenguinSprite penguin;
	ArcadeCompressor compressor;

	ImageSprite nextBubble;
	int currentColor, nextColor;

	ArcadeBubbleSprite movingBubble;
	BubbleManager bubbleManager;
	ArcadeLevelManager levelManager;
	// TODO
	// HighscoreManager highscoreManager;

	Vector jumping;
	Vector falling;

	ArcadeBubbleSprite[][] bubblePlay;

	int fixedBubbles;
	double moveDown;

	BmpWrap gameWon, gameLost;

	int nbBubbles;

	BmpWrap bubbleBlink;
	int blinkDelay;

	ImageSprite hurrySprite;
	int hurryTime;

	SoundManager soundManager;

	boolean readyToFire;
	boolean endOfGame;
	boolean frozenify;
	int frozenifyX, frozenifyY;

	Drawable launcher;
	BmpWrap penguins;
	private float moveDownDelta;

	public ArcadeGame(BmpWrap background_arg, BmpWrap[] bubbles_arg,
			BmpWrap[] bubblesBlind_arg, BmpWrap[] frozenBubbles_arg,
			BmpWrap[] targetedBubbles_arg, BmpWrap bubbleBlink_arg,
			BmpWrap gameWon_arg, BmpWrap gameLost_arg, BmpWrap hurry_arg,
			BmpWrap penguins_arg, BmpWrap compressorHead_arg,
			BmpWrap compressor_arg, Drawable launcher_arg,
			SoundManager soundManager_arg, ArcadeLevelManager levelManager_arg) {
		random = new Random(System.currentTimeMillis());
		launcher = launcher_arg;
		penguins = penguins_arg;
		background = background_arg;
		bubbles = bubbles_arg;
		bubblesBlind = bubblesBlind_arg;
		frozenBubbles = frozenBubbles_arg;
		targetedBubbles = targetedBubbles_arg;
		bubbleBlink = bubbleBlink_arg;
		gameWon = gameWon_arg;
		gameLost = gameLost_arg;
		soundManager = soundManager_arg;

		// /////////////////////////////////////////////
		//
		// important
		//
		// /////////////////////////////////////////////
		levelManager = levelManager_arg;
		levelRows = levelManager.getLevelRows();
		moveDownDelta = levelManager.getCurrentSpeed();

		// /////////////////////////////////////////////

		launchBubblePosition = 20;

		penguin = new PenguinSprite(penguins_arg, random);
		this.addSprite(penguin);
		compressor = new ArcadeCompressor(compressorHead_arg, compressor_arg,
				380.0f - 28.0f * levelRows);

		hurrySprite = new ImageSprite(new Rect(203, 265, 203 + 240, 265 + 90),
				hurry_arg);

		jumping = new Vector();
		falling = new Vector();

		bubblePlay = new ArcadeBubbleSprite[8][levelRows + 1];

		bubbleManager = new BubbleManager(bubbles);
		byte[][] currentLevel = levelManager.getCurrentLevel();

		if (currentLevel == null) {
			// Log.i("frozen-bubble", "Level not available.");
			return;
		}

		int cols = levelManager.getWidth();

		for (int j = 0; j < levelRows; j++) {
			for (int i = j % 2; i < cols; i++) {
				if (currentLevel[i][j] != -1) {
					ArcadeBubbleSprite newOne = new ArcadeBubbleSprite(
							new Rect(190 + i * 32 - (j % 2) * 16,
									380 - 28 * (levelRows - j), 32, 32),
							currentLevel[i][j], bubbles[currentLevel[i][j]],
							bubblesBlind[currentLevel[i][j]],
							frozenBubbles[currentLevel[i][j]], bubbleBlink,
							bubbleManager, soundManager, this);
					bubblePlay[i][j] = newOne;
					this.addSprite(newOne);
				}
			}
		}

		currentColor = bubbleManager.nextBubbleIndex(random);
		nextColor = bubbleManager.nextBubbleIndex(random);

		if (BubbleArcadeActivity.getMode() == BubbleArcadeActivity.GAME_NORMAL) {
			nextBubble = new ImageSprite(
					new Rect(302, 440, 302 + 32, 440 + 32), bubbles[nextColor]);
		} else {
			nextBubble = new ImageSprite(
					new Rect(302, 440, 302 + 32, 440 + 32),
					bubblesBlind[nextColor]);
		}
		this.addSprite(nextBubble);

		launchBubble = new LaunchBubbleSprite(currentColor,
				(int) launchBubblePosition, launcher, bubbles, bubblesBlind);

		this.spriteToBack(launchBubble);

		nbBubbles = 0;
	}

	public void saveState(Bundle map) {
		Vector savedSprites = new Vector();
		saveSprites(map, savedSprites);
		for (int i = 0; i < jumping.size(); i++) {
			((Sprite) jumping.elementAt(i)).saveState(map, savedSprites);
			map.putInt(String.format("jumping-%d", i),
					((Sprite) jumping.elementAt(i)).getSavedId());
		}
		map.putInt("numJumpingSprites", jumping.size());
		for (int i = 0; i < falling.size(); i++) {
			((Sprite) falling.elementAt(i)).saveState(map, savedSprites);
			map.putInt(String.format("falling-%d", i),
					((Sprite) falling.elementAt(i)).getSavedId());
		}
		map.putInt("numFallingSprites", falling.size());
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < levelRows; j++) {
				if (bubblePlay[i][j] != null) {
					bubblePlay[i][j].saveState(map, savedSprites);
					map.putInt(String.format("play-%d-%d", i, j),
							bubblePlay[i][j].getSavedId());
				} else {
					map.putInt(String.format("play-%d-%d", i, j), -1);
				}
			}
		}
		launchBubble.saveState(map, savedSprites);
		map.putInt("launchBubbleId", launchBubble.getSavedId());
		map.putDouble("launchBubblePosition", launchBubblePosition);
		penguin.saveState(map, savedSprites);
		compressor.saveState(map);
		map.putInt("penguinId", penguin.getSavedId());
		nextBubble.saveState(map, savedSprites);
		map.putInt("nextBubbleId", nextBubble.getSavedId());
		map.putInt("currentColor", currentColor);
		map.putInt("nextColor", nextColor);
		if (movingBubble != null) {
			movingBubble.saveState(map, savedSprites);
			map.putInt("movingBubbleId", movingBubble.getSavedId());
		} else {
			map.putInt("movingBubbleId", -1);
		}
		bubbleManager.saveState(map);
		map.putInt("fixedBubbles", fixedBubbles);
		map.putDouble("moveDown", moveDown);
		map.putInt("nbBubbles", nbBubbles);
		map.putInt("blinkDelay", blinkDelay);
		hurrySprite.saveState(map, savedSprites);
		map.putInt("hurryId", hurrySprite.getSavedId());
		map.putInt("hurryTime", hurryTime);
		map.putBoolean("readyToFire", readyToFire);
		map.putBoolean("endOfGame", endOfGame);
		map.putBoolean("frozenify", frozenify);
		map.putInt("frozenifyX", frozenifyX);
		map.putInt("frozenifyY", frozenifyY);

		map.putInt("numSavedSprites", savedSprites.size());

		for (int i = 0; i < savedSprites.size(); i++) {
			((Sprite) savedSprites.elementAt(i)).clearSavedId();
		}
	}

	private Sprite restoreSprite(Bundle map, Vector imageList, int i) {
		int left = map.getInt(String.format("%d-left", i));
		int right = map.getInt(String.format("%d-right", i));
		int top = map.getInt(String.format("%d-top", i));
		int bottom = map.getInt(String.format("%d-bottom", i));
		int type = map.getInt(String.format("%d-type", i));
		if (type == Sprite.TYPE_BUBBLE) {
			int color = map.getInt(String.format("%d-color", i));
			double moveX = map.getDouble(String.format("%d-moveX", i));
			double moveY = map.getDouble(String.format("%d-moveY", i));
			double realX = map.getDouble(String.format("%d-realX", i));
			double realY = map.getDouble(String.format("%d-realY", i));
			boolean fixed = map.getBoolean(String.format("%d-fixed", i));
			boolean blink = map.getBoolean(String.format("%d-blink", i));
			boolean released = map.getBoolean(String.format("%d-released", i));
			boolean checkJump = map
					.getBoolean(String.format("%d-checkJump", i));
			boolean checkFall = map
					.getBoolean(String.format("%d-checkFall", i));
			int fixedAnim = map.getInt(String.format("%d-fixedAnim", i));
			boolean frozen = map.getBoolean(String.format("%d-frozen", i));
			return new ArcadeBubbleSprite(new Rect(left, top, right, bottom),
					color, moveX, moveY, realX, realY, fixed, blink, released,
					checkJump, checkFall, fixedAnim,
					(frozen ? frozenBubbles[color] : bubbles[color]),
					bubblesBlind[color], frozenBubbles[color], targetedBubbles,
					bubbleBlink, bubbleManager, soundManager, this);
		} else if (type == Sprite.TYPE_IMAGE) {
			int imageId = map.getInt(String.format("%d-imageId", i));
			return new ImageSprite(new Rect(left, top, right, bottom),
					(BmpWrap) imageList.elementAt(imageId));
		} else if (type == Sprite.TYPE_LAUNCH_BUBBLE) {
			int currentColor = map.getInt(String.format("%d-currentColor", i));
			int currentDirection = map.getInt(String.format(
					"%d-currentDirection", i));
			return new LaunchBubbleSprite(currentColor, currentDirection,
					launcher, bubbles, bubblesBlind);
		} else if (type == Sprite.TYPE_PENGUIN) {
			int currentPenguin = map.getInt(String.format("%d-currentPenguin",
					i));
			int count = map.getInt(String.format("%d-count", i));
			int finalState = map.getInt(String.format("%d-finalState", i));
			int nextPosition = map.getInt(String.format("%d-nextPosition", i));
			return new PenguinSprite(penguins, random, currentPenguin, count,
					finalState, nextPosition);
		} else {
			Log.e("frozen-bubble", "Unrecognized sprite type: " + type);
			return null;
		}
	}

	public void restoreState(Bundle map, Vector imageList) {
		Vector savedSprites = new Vector();
		int numSavedSprites = map.getInt("numSavedSprites");
		for (int i = 0; i < numSavedSprites; i++) {
			savedSprites.addElement(restoreSprite(map, imageList, i));
		}

		restoreSprites(map, savedSprites);
		jumping = new Vector();
		int numJumpingSprites = map.getInt("numJumpingSprites");
		for (int i = 0; i < numJumpingSprites; i++) {
			int spriteIdx = map.getInt(String.format("jumping-%d", i));
			jumping.addElement(savedSprites.elementAt(spriteIdx));
		}
		falling = new Vector();
		int numFallingSprites = map.getInt("numFallingSprites");
		for (int i = 0; i < numFallingSprites; i++) {
			int spriteIdx = map.getInt(String.format("falling-%d", i));
			falling.addElement(savedSprites.elementAt(spriteIdx));
		}
		bubblePlay = new ArcadeBubbleSprite[8][levelRows+1];
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < levelRows; j++) {
				int spriteIdx = map.getInt(String.format("play-%d-%d", i, j));
				if (spriteIdx != -1) {
					bubblePlay[i][j] = (ArcadeBubbleSprite) savedSprites
							.elementAt(spriteIdx);
				} else {
					bubblePlay[i][j] = null;
				}
			}
		}
		int launchBubbleId = map.getInt("launchBubbleId");
		launchBubble = (LaunchBubbleSprite) savedSprites
				.elementAt(launchBubbleId);
		launchBubblePosition = map.getDouble("launchBubblePosition");
		int penguinId = map.getInt("penguinId");
		penguin = (PenguinSprite) savedSprites.elementAt(penguinId);
		compressor.restoreState(map);
		int nextBubbleId = map.getInt("nextBubbleId");
		nextBubble = (ImageSprite) savedSprites.elementAt(nextBubbleId);
		currentColor = map.getInt("currentColor");
		nextColor = map.getInt("nextColor");
		int movingBubbleId = map.getInt("movingBubbleId");
		if (movingBubbleId == -1) {
			movingBubble = null;
		} else {
			movingBubble = (ArcadeBubbleSprite) savedSprites
					.elementAt(movingBubbleId);
		}
		bubbleManager.restoreState(map);
		fixedBubbles = map.getInt("fixedBubbles");
		moveDown = map.getDouble("moveDown");
		nbBubbles = map.getInt("nbBubbles");
		blinkDelay = map.getInt("blinkDelay");
		int hurryId = map.getInt("hurryId");
		hurrySprite = (ImageSprite) savedSprites.elementAt(hurryId);
		hurryTime = map.getInt("hurryTime");
		readyToFire = map.getBoolean("readyToFire");
		endOfGame = map.getBoolean("endOfGame");
		frozenify = map.getBoolean("frozenify");
		frozenifyX = map.getInt("frozenifyX");
		frozenifyY = map.getInt("frozenifyY");
	}

	private void initFrozenify() {
		ImageSprite freezeLaunchBubble = new ImageSprite(new Rect(301, 389, 34,
				42), frozenBubbles[currentColor]);
		ImageSprite freezeNextBubble = new ImageSprite(new Rect(301, 439, 34,
				42), frozenBubbles[nextColor]);

		this.addSprite(freezeLaunchBubble);
		this.addSprite(freezeNextBubble);

		frozenifyX = 7;
		frozenifyY = levelRows;

		frozenify = true;
	}

	private void frozenify() {
		frozenifyX--;
		if (frozenifyX < 0) {
			frozenifyX = 7;
			frozenifyY--;

			if (frozenifyY < 0) {
				frozenify = false;
				this.addSprite(new ImageSprite(new Rect(152, 190, 337, 116),
						gameLost));
				soundManager.playSound(BubbleArcadeActivity.SOUND_NOH);

				return;
			}
		}

		while (bubblePlay[frozenifyX][frozenifyY] == null && frozenifyY >= 0) {
			frozenifyX--;
			if (frozenifyX < 0) {
				frozenifyX = 7;
				frozenifyY--;

				if (frozenifyY < 0) {
					frozenify = false;
					this.addSprite(new ImageSprite(
							new Rect(152, 190, 337, 116), gameLost));
					soundManager.playSound(BubbleArcadeActivity.SOUND_NOH);

					return;
				}
			}
		}

		this.spriteToBack(bubblePlay[frozenifyX][frozenifyY]);
		bubblePlay[frozenifyX][frozenifyY].frozenify();

		this.spriteToBack(launchBubble);
	}

	public ArcadeBubbleSprite[][] getGrid() {
		return bubblePlay;
	}

	public void addFallingBubble(ArcadeBubbleSprite sprite) {
		spriteToFront(sprite);
		falling.addElement(sprite);
	}

	public void deleteFallingBubble(ArcadeBubbleSprite sprite) {
		removeSprite(sprite);
		falling.removeElement(sprite);
	}

	public void addJumpingBubble(ArcadeBubbleSprite sprite) {
		spriteToFront(sprite);
		jumping.addElement(sprite);
	}

	public void deleteJumpingBubble(ArcadeBubbleSprite sprite) {
		removeSprite(sprite);
		jumping.removeElement(sprite);
	}

	public Random getRandom() {
		return random;
	}

	public double getMoveDown() {
		return moveDown;
	}

	/*
	 * private int nextColor() { int nextColor = random.nextInt() % 8;
	 * 
	 * if (nextColor<0) { return -nextColor; }
	 * 
	 * return nextColor; }
	 */
	private void moveDown() {

		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < levelRows; j++) {
				if (bubblePlay[i][j] != null) {
					bubblePlay[i][j].moveDown(moveDownDelta);

					if (bubblePlay[i][j].getSpritePosition().y >= MIN_LINE_Y) {
						gameLost();
					}
				}
			}
		}

		moveDown += moveDownDelta;
		compressor.moveDown(moveDownDelta);
	}

	private void gameLost() {
		penguin.updateState(PenguinSprite.STATE_GAME_LOST);
		endOfGame = true;
		initFrozenify();

		ScoreManager.getInstance().clearTempScore();

//		levelManager.goToFirstLevel();
		
		soundManager.playSound(BubbleArcadeActivity.SOUND_LOST);
	}
	
	private void gameWin(){
		penguin.updateState(PenguinSprite.STATE_GAME_WON);
		this.addSprite(new ImageSprite(new Rect(152, 190, 337,
				116), gameWon));

		levelCompleted = true;
		endOfGame = true;

		soundManager.playSound(BubbleArcadeActivity.SOUND_WON);

		
		ScoreManager.getInstance().addToTotalScore();
		ScoreManager.getInstance().clearTempScore();
	}

	private void blinkLine(int number) {
		int move = number % 2;
		int column = (number + 1) >> 1;

		for (int i = move; i < levelRows + 1; i++) {
			if (bubblePlay[column][i] != null) {
				bubblePlay[column][i].blink();
			}
		}
	}

	public enum gameState {
		RUNING, LOST, WIN;
	}

	public boolean play(boolean key_left, boolean key_right, boolean key_fire,
			double trackball_dx, double[] touch_dx) {
		int[] move = new int[2];

		if (key_left && !key_right) {
			move[HORIZONTAL_MOVE] = KEY_LEFT;
		} else if (key_right && !key_left) {
			move[HORIZONTAL_MOVE] = KEY_RIGHT;
		} else {
			move[HORIZONTAL_MOVE] = 0;
		}
		if (key_fire) {
			move[FIRE] = KEY_UP;
		} else {
			move[FIRE] = 0;
		}

		if (move[FIRE] == 0) {
			readyToFire = true;
		}

		if (BubbleArcadeActivity.getDontRushMe()) {
			hurryTime = 1;
		}

		if (key_fire && touch_dx != null) {
			double x_r = -(touch_dx[0] - launchBubble.getXCenter());
			double y_r = touch_dx[1] - launchBubble.getYCenter();

			launchBubblePosition = Math.acos(x_r
					/ Math.sqrt(x_r * x_r + y_r * y_r))
					* 40 / Math.PI;

			if (launchBubblePosition < 1) {
				launchBubblePosition = 1;
			}
			if (launchBubblePosition > 39) {
				launchBubblePosition = 39;
			}
			// Log.i("sloan","position:"+launchBubblePosition);
			launchBubble.changeDirection((int) launchBubblePosition);
		}

		if (endOfGame) {
			if (move[FIRE] == KEY_UP && readyToFire) {
				if (levelCompleted) {
					levelManager.goToNextLevel();
				}
				return true;
				// return gameState.WIN;
			} else {
				penguin.updateState(PenguinSprite.STATE_VOID);

				if (frozenify) {
					frozenify();
				}
			}
		} else {
			if (move[FIRE] == KEY_UP || hurryTime > 480) {
				if (movingBubble == null && readyToFire) {
					nbBubbles++;

					movingBubble = new ArcadeBubbleSprite(new Rect(302, 390,
							32, 32), (int) launchBubblePosition, currentColor,
							bubbles[currentColor], bubblesBlind[currentColor],
							frozenBubbles[currentColor], targetedBubbles,
							bubbleBlink, bubbleManager, soundManager, this);
					this.addSprite(movingBubble);

					currentColor = nextColor;
					nextColor = bubbleManager.nextBubbleIndex(random);

					if (BubbleArcadeActivity.getMode() == BubbleArcadeActivity.GAME_NORMAL) {
						nextBubble.changeImage(bubbles[nextColor]);
					} else {
						nextBubble.changeImage(bubblesBlind[nextColor]);
					}
					launchBubble.changeColor(currentColor);
					penguin.updateState(PenguinSprite.STATE_FIRE);

					soundManager.playSound(BubbleArcadeActivity.SOUND_LAUNCH);

					readyToFire = false;
					hurryTime = 0;
					removeSprite(hurrySprite);
				} else {
					penguin.updateState(PenguinSprite.STATE_VOID);
				}
			} else {
				double dx = 0;
				if (move[HORIZONTAL_MOVE] == KEY_LEFT) {
					dx -= 1;
				}
				if (move[HORIZONTAL_MOVE] == KEY_RIGHT) {
					dx += 1;
				}
				dx += trackball_dx;
				// dx += touch_dx;
				launchBubblePosition += dx;
				if (launchBubblePosition < 1) {
					launchBubblePosition = 1;
				}
				if (launchBubblePosition > 39) {
					launchBubblePosition = 39;
				}
				launchBubble.changeDirection((int) launchBubblePosition);
				if (dx < 0) {
					penguin.updateState(PenguinSprite.STATE_TURN_LEFT);
				} else if (dx > 0) {
					penguin.updateState(PenguinSprite.STATE_TURN_RIGHT);
				} else {
					penguin.updateState(PenguinSprite.STATE_VOID);
				}
			}

			moveDown();
		}

		if (movingBubble != null) {
			movingBubble.move(levelRows);
			if (movingBubble.fixed()) {
				if (movingBubble.getSpritePosition().y >= MIN_LINE_Y
						&& !movingBubble.released()) {

					gameLost();

				} else if (bubbleManager.countBubbles() == 0) {
					
					gameWin();
					
				} else {
					fixedBubbles++;
					blinkDelay = 0;

					if (fixedBubbles == 8) {
						fixedBubbles = 0;
					}
				}
				movingBubble = null;
			}

			if (movingBubble != null) {
				movingBubble.move(levelRows);
				if (movingBubble.fixed()) {
					if (movingBubble.getSpritePosition().y >= MIN_LINE_Y
							&& !movingBubble.released()) {

						gameLost();

					} else if (bubbleManager.countBubbles() == 0) {
						
						gameWin();
						
					} else {
						fixedBubbles++;
						blinkDelay = 0;

						if (fixedBubbles == 8) {
							fixedBubbles = 0;
						}
					}
					movingBubble = null;
				}

			}
		}

		if (movingBubble == null && !endOfGame) {
			hurryTime++;
			// If hurryTime == 2 (1 + 1) we could be in the "Don't rush me"
			// mode. Remove the sprite just in case the user switched
			// to this mode when the "Hurry" sprite was shown, to make it
			// disappear.
			if (hurryTime == 2) {
				removeSprite(hurrySprite);
			}
			if (hurryTime >= 240) {
				if (hurryTime % 40 == 10) {
					addSprite(hurrySprite);
					soundManager.playSound(BubbleArcadeActivity.SOUND_HURRY);
				} else if (hurryTime % 40 == 35) {
					removeSprite(hurrySprite);
				}
			}
		}

		if (fixedBubbles == 6) {
			if (blinkDelay < 15) {
				blinkLine(blinkDelay);
			}

			blinkDelay++;
			if (blinkDelay == 40) {
				blinkDelay = 0;
			}
		} else if (fixedBubbles == 7) {
			if (blinkDelay < 15) {
				blinkLine(blinkDelay);
			}

			blinkDelay++;
			if (blinkDelay == 25) {
				blinkDelay = 0;
			}
		}


		for (int i = 0; i < falling.size(); i++) {
			((ArcadeBubbleSprite) falling.elementAt(i)).fall();
		}
		for (int i = 0; i < jumping.size(); i++) {
			((ArcadeBubbleSprite) jumping.elementAt(i)).jump();
		}
		// return gameState.RUNING;
		return false;
	}

	public void paint(Canvas c, double scale, int dx, int dy) {
		compressor.paint(c, scale, dx, dy);
		// if (BubbleArcadeActivity.getMode() ==
		// BubbleArcadeActivity.GAME_NORMAL) {
		nextBubble.changeImage(bubbles[nextColor]);
		/*
		 * } else { nextBubble.changeImage(bubblesBlind[nextColor]); }
		 */
		super.paint(c, scale, dx, dy);
	}
}
