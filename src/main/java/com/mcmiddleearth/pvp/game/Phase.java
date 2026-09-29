package com.mcmiddleearth.pvp.game;

/** WAITING → COUNTDOWN → RUNNING → ENDING → CLOSED; CLOSED can also follow any phase (stop, shutdown). */
public enum Phase { WAITING, COUNTDOWN, RUNNING, ENDING, CLOSED }
