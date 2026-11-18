"""Generate GeoTank's sound effects from scratch (no third-party audio).

Run: python3 tools/make_sfx.py  ->  writes assets/sounds/effects/*.wav
"""
import pathlib
import wave

import numpy as np

SR = 44100
OUT = pathlib.Path(__file__).resolve().parent.parent / "assets" / "sounds" / "effects"
rng = np.random.default_rng(7)


def t(sec):
    return np.arange(int(SR * sec)) / SR


def env(n, attack=0.005, release=1.0, curve=3.0):
    """Fast attack, exponential-ish decay over the whole sample."""
    x = np.linspace(0, 1, n)
    a = np.minimum(1, np.arange(n) / max(1, int(attack * SR)))
    return a * (1 - x) ** curve * release


def square(freq, tt):
    phase = np.cumsum(freq / SR) if np.ndim(freq) else freq * tt
    return np.sign(np.sin(2 * np.pi * phase))


def lowpass(x, alpha):
    y = np.empty_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def write(name, x, gain=0.6):
    x = x / (np.max(np.abs(x)) or 1) * gain
    data = (x * 32767).astype("<i2").tobytes()
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data)
    print("wrote", OUT / name, f"{len(x) / SR:.2f}s")


def shoot():
    tt = t(0.22)
    n = len(tt)
    noise = lowpass(rng.uniform(-1, 1, n), 0.25) * env(n, curve=4)
    freq = np.geomspace(180, 45, n)
    thump = np.sin(2 * np.pi * np.cumsum(freq) / SR) * env(n, curve=2.5)
    return 0.7 * noise + thump


def level_up():
    notes = [523.25, 659.25, 783.99, 1046.5]  # C5 E5 G5 C6
    parts = []
    for i, f in enumerate(notes):
        dur = 0.09 if i < 3 else 0.35
        tt = t(dur)
        tone = 0.6 * square(f, tt) + 0.4 * np.sin(2 * np.pi * 2 * f * tt)
        parts.append(tone * env(len(tt), curve=1.5 if i < 3 else 2.5))
    return lowpass(np.concatenate(parts), 0.35)


def upgrade():
    parts = []
    for f, dur in [(880.0, 0.07), (1318.5, 0.22)]:  # A5 -> E6
        tt = t(dur)
        tone = np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * 3 * f * tt)
        parts.append(tone * env(len(tt), curve=2))
    return np.concatenate(parts)


def death():
    tt = t(0.75)
    n = len(tt)
    freq = np.geomspace(420, 55, n)
    sweep = square(freq, tt) * 0.5
    noise = lowpass(rng.uniform(-1, 1, n), 0.12)
    return lowpass(sweep + noise, 0.3) * env(n, curve=1.8)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    write("shoot.wav", shoot(), gain=0.55)
    write("levelup.wav", level_up(), gain=0.45)
    write("upgrade.wav", upgrade(), gain=0.45)
    write("death.wav", death(), gain=0.55)
