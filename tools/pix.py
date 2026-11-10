"""
A small pixel-art painter for the Songling pets (a numpy port of Familiar's ui/pix): shapes lit through
their surface normals with hue-shifted ramps, ordered dither between tones, selective outlines, rim
light. Everything is drawn in design units: a 100 x 100 grid whose ground (y = 88) is the sprite's
bottom row, scaled by the pet's stage.
"""
import numpy as np
from PIL import Image

N = 80
_Y, _X = np.mgrid[0:N, 0:N].astype(np.float32) + 0.5
_L = np.array([-0.55, -0.75, 0.65], np.float32)
_L /= np.linalg.norm(_L)
_B = (np.array([0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5], np.float32).reshape(4, 4) + 0.5) / 16 - 0.5
BAYER = _B[np.arange(N)[:, None] & 3, np.arange(N)[None, :] & 3]
# Soft fur-like grain: hash noise, a little smeared down so it reads as strands.
_rng = np.random.default_rng(42)
_g = _rng.random((N + 2, N)).astype(np.float32)
GRAIN = ((_g[:-2] + _g[1:-1] + _g[2:]) / 3 - 0.5) * 0.16


def hx(s):
    s = s.lstrip("#")
    return np.array([int(s[i:i + 2], 16) for i in (0, 2, 4)], np.float32)


def mix(a, b, t):
    return np.asarray(a, np.float32) + (np.asarray(b, np.float32) - np.asarray(a, np.float32)) * t


class Ramp:
    def __init__(self, *cols):
        self.c = np.array([hx(c) if isinstance(c, str) else c for c in cols], np.float32)

    def __len__(self):
        return len(self.c)

    def __getitem__(self, i):
        return self.c[max(0, min(len(self.c) - 1, i))]

    @staticmethod
    def around(base, dark="#1e0e28", warm="#fff6d6"):
        b, d, w = hx(base) if isinstance(base, str) else base, hx(dark), hx(warm)
        return Ramp(mix(b, d, .78), mix(b, d, .56), mix(b, d, .32), mix(b, d, .12), b, mix(b, w, .35), mix(b, w, .68))


OUT = hx("1a0e22")
WHITE = hx("fffcf4")
EYE = Ramp("140c1e", "1e122a", "2c1e40", "40305a")


class Px:
    def __init__(self):
        self.c = np.zeros((N, N, 3), np.float32)
        self.a = np.zeros((N, N), np.float32)

    def put(self, mask, col, alpha=255.0):
        if not mask.any():
            return
        col = np.broadcast_to(np.asarray(col, np.float32), (N, N, 3)) if np.ndim(col) == 1 else col
        al = np.broadcast_to(np.asarray(alpha, np.float32), (N, N))
        full = mask & ((al >= 255) | (self.a == 0))
        self.c[full] = col[full]
        self.a[full] = np.maximum(np.where(self.a[full] == 0, al[full], 255), 0)
        part = mask & ~full & (al > 0)
        if part.any():
            t = (al[part] / 255.0)[:, None]
            self.c[part] = self.c[part] + (col[part] - self.c[part]) * t
            self.a[part] = np.maximum(self.a[part], al[part])

    def dot(self, x, y, col, alpha=255.0):
        x, y = int(round(x - 0.5)), int(round(y - 0.5))
        if 0 <= x < N and 0 <= y < N:
            m = np.zeros((N, N), bool)
            m[y, x] = True
            self.put(m, col, alpha)

    def solid(self, t=160):
        return self.a >= t

    def ring(self, col=OUT):
        m = self.solid()
        r = np.zeros_like(m)
        r[:, 1:] |= m[:, :-1]
        r[:, :-1] |= m[:, 1:]
        r[1:, :] |= m[:-1, :]
        r[:-1, :] |= m[1:, :]
        self.put(r & ~m, col)

    def image(self, scale=1):
        im = np.dstack([np.clip(self.c, 0, 255), np.clip(self.a, 0, 255)]).astype(np.uint8)
        img = Image.fromarray(im, "RGBA")
        return img.resize((N * scale, N * scale), Image.NEAREST) if scale != 1 else img


def edges(m):
    """Edge flags of a mask: right, left, bottom, top neighbour outside."""
    r = np.ones_like(m); r[:, :-1] = ~m[:, 1:]
    l = np.ones_like(m); l[:, 1:] = ~m[:, :-1]
    b = np.ones_like(m); b[:-1, :] = ~m[1:, :]
    t = np.ones_like(m); t[1:, :] = ~m[:-1, :]
    return r & m, l & m, b & m, t & m


class Pen:
    """Design units -> pixels. [s] scales for the stage, [dy] lifts (pixels) for floating."""

    def __init__(self, px, s=1.0, dx=0.0, dy=0.0):
        self.px, self.s = px, s
        self.k = N / 100.0 * s
        ground = N - 3.0
        self.U = (_X - N / 2.0 - dx) / self.k + 50.0
        self.V = (_Y - ground - dy) / self.k + 88.0
        self._ground, self._dx, self._dy = ground, dx, dy

    # design -> pixel
    def x(self, u):
        return N / 2.0 + (u - 50.0) * self.k + self._dx

    def y(self, v):
        return self._ground + (v - 88.0) * self.k + self._dy

    # --- shapes (masks) ---
    def ell(self, cx, cy, rx, ry=None, ang=0.0):
        ry = rx if ry is None else ry
        u, v = self._rot(cx, cy, ang)
        return (u / rx) ** 2 + (v / ry) ** 2 <= 1.0

    def _rot(self, cx, cy, ang):
        a = np.radians(ang)
        du, dv = self.U - cx, self.V - cy
        return du * np.cos(a) + dv * np.sin(a), -du * np.sin(a) + dv * np.cos(a)

    def box(self, x0, y0, x1, y1):
        return (self.U >= x0) & (self.U < x1) & (self.V >= y0) & (self.V < y1)

    def poly(self, pts):
        xs = np.array(pts[0::2], np.float32)
        ys = np.array(pts[1::2], np.float32)
        inside = np.zeros((N, N), bool)
        j = len(xs) - 1
        for i in range(len(xs)):
            c = ((ys[i] > self.V) != (ys[j] > self.V)) & (self.U < (xs[j] - xs[i]) * (self.V - ys[i]) / (ys[j] - ys[i] + 1e-6) + xs[i])
            inside ^= c
            j = i
        return inside

    def stroke(self, pts, w0, w1=None, n=None):
        """A tube along a polyline / sampled curve [pts] (list of (x, y)), width tapering w0 -> w1.
        Returns (mask, normal)."""
        w1 = w0 if w1 is None else w1
        pts = np.array(pts, np.float32)
        # resample densely
        seg = np.sqrt(((pts[1:] - pts[:-1]) ** 2).sum(1))
        total = seg.sum()
        n = n or max(8, int(total * self.k * 1.5))
        cum = np.concatenate([[0], np.cumsum(seg)])
        ts = np.linspace(0, total, n)
        sx = np.interp(ts, cum, pts[:, 0]); sy = np.interp(ts, cum, pts[:, 1])
        ws = np.linspace(w0, w1, n)
        best = np.full((N, N), 9e9, np.float32); bu = np.zeros((N, N), np.float32); bv = np.zeros((N, N), np.float32); bw = np.ones((N, N), np.float32)
        for i in range(n):
            du, dv = self.U - sx[i], self.V - sy[i]
            d = np.sqrt(du * du + dv * dv) / ws[i]
            m = d < best
            best = np.where(m, d, best); bu = np.where(m, du, bu); bv = np.where(m, dv, bv); bw = np.where(m, ws[i], bw)
        mask = best <= 1.0
        nx = np.clip(bu / bw, -1, 1); ny = np.clip(bv / bw, -1, 1)
        nz = np.sqrt(np.maximum(0, 1 - nx * nx - ny * ny))
        return mask, _norm(nx, ny, nz)

    # --- normals ---
    def dome(self, cx, cy, rx, ry=None, ang=0.0, flat=0.0):
        ry = rx if ry is None else ry
        u, v = self._rot(cx, cy, ang)
        dx = np.clip(u / rx, -1, 1); dy = np.clip(v / ry, -1, 1)
        z = np.sqrt(np.maximum(0, 1 - dx * dx - dy * dy)) + flat
        a = np.radians(ang)
        nx = dx * np.cos(a) - dy * np.sin(a); ny = dx * np.sin(a) + dy * np.cos(a)
        return _norm(nx, ny, z)

    def face(self, nx=0.0, ny=0.0, nz=1.0):
        return _norm(np.full((N, N), nx, np.float32), np.full((N, N), ny, np.float32), np.full((N, N), nz, np.float32))

    def column(self, cx, rx):
        dx = np.clip((self.U - cx) / rx, -1, 1)
        return _norm(dx, np.zeros_like(dx), np.sqrt(np.maximum(0, 1 - dx * dx)))

    def log(self, cy, ry):
        dy = np.clip((self.V - cy) / ry, -1, 1)
        return _norm(np.zeros_like(dy), dy, np.sqrt(np.maximum(0, 1 - dy * dy)))

    # --- painting ---
    def shade(self, mask, normal, ramp, outline=True, dither=0.45, ambient=0.22, tex=None, rim=None, lo=0, hi=None, line=None, spec=False, alpha=255.0):
        if not mask.any():
            return mask
        hi = len(ramp) - 1 if hi is None else hi
        nx, ny, nz = normal
        d = nx * _L[0] + ny * _L[1] + nz * _L[2]
        l = np.maximum(d, 0) * (1 - ambient) + ambient
        if tex is None:
            tex = GRAIN
        if tex is not None and not (np.isscalar(tex) and tex == 0):
            l = l + tex
        g = np.clip(l, 0, 1) ** 1.35
        v = lo + g * (hi - lo - 0.45) + BAYER * dither
        idx = np.clip(np.rint(v), lo, hi).astype(int)
        col = ramp.c[idx]
        if spec:
            col = np.where((d > 0.95)[..., None], mix(ramp[len(ramp) - 1], WHITE, .5), col)
        self.px.put(mask, col, alpha)
        if outline:
            r, lf, b, t = edges(mask)
            e = r | lf | b | t
            lit = (t | lf) & ~(r | b)
            dark = ramp[0] if line is None else line
            self.px.put(e & lit, ramp[1], alpha)
            self.px.put(e & ~lit, dark, alpha)
            if rim is not None:
                inner = np.zeros_like(mask)
                rr = r & ~(t | lf)
                bb = b & ~(t | lf)
                inner[:, :-1] |= rr[:, 1:]
                inner[:-1, :] |= bb[1:, :]
                inner &= mask & ~e
                self.px.put(inner, rim, 130)
        return mask

    def fill(self, mask, col, alpha=255.0):
        self.px.put(mask, hx(col) if isinstance(col, str) else col, alpha)
        return mask

    def glow(self, cx, cy, r, col, strength=90, steps=4, only=None):
        d = np.sqrt((self.U - cx) ** 2 + (self.V - cy) ** 2) / r
        st = np.floor((1 - d) * steps + BAYER * 0.8) / steps
        m = (d < 1) & (st > 0)
        if only is not None:
            m &= only
        self.px.put(m, hx(col) if isinstance(col, str) else col, np.clip(strength * st, 0, 255))

    def eye(self, cx, cy, r, ry=None, look=0.0, iris="7a4a30"):
        """A big glossy eye: dark rim, a coloured iris, a pupil and two shines. Returns its pixel box."""
        ry = r * 1.15 if ry is None else ry
        m = self.ell(cx, cy, r, ry)
        self.shade(m, self.dome(cx, cy, r, ry), EYE, outline=False, dither=0.2, tex=0)
        ys, xs = np.nonzero(m)
        if len(xs) == 0:
            return None
        x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
        w = x1 - x0 + 1
        if w >= 5:
            # The iris, lit from below, and the pupil.
            ir = Ramp.around(iris)
            im = self.ell(cx, cy + ry * 0.18, r * 0.72, ry * 0.7) & m
            self.shade(im, self.dome(cx, cy + ry * 0.35, r * 0.72, ry * 0.7), ir, outline=False, dither=0.3, lo=2, tex=0)
            self.px.put(self.ell(cx, cy + ry * 0.2, r * 0.4, ry * 0.45) & m, EYE[0])
        sh = 3 if w >= 8 else 2 if w >= 5 else 1
        sx, sy = x0 + max(0, int(round((w - sh) * 0.22 + look))), y0 + max(0, int(round((y1 - y0 + 1) * 0.14)))
        sm = np.zeros((N, N), bool)
        sm[sy:sy + sh, sx:sx + sh] = True
        self.px.put(sm & m, WHITE)
        if w >= 5:
            self.px.put(_one(x1 - 1 - (1 if w >= 8 else 0), y1 - 1 - (1 if w >= 8 else 0)) & m, WHITE, 230)
        return (int(x0), int(y0), int(x1 + 1), int(y1 + 1))

    def mouth(self, cx, cy, w, open_=True):
        """A happy open mouth: a dark smile with a pink tongue in it."""
        m = self.ell(cx, cy, w, w * 0.7) & (self.V > cy - w * 0.1)
        self.fill(m, "3a1424")
        if open_:
            self.fill(self.ell(cx, cy + w * 0.45, w * 0.6, w * 0.35) & m, "e8708a")

    def cheek(self, cx, cy, r, col="f08aa0", alpha=120):
        self.px.put(self.ell(cx, cy, r, r * 0.6) & self.px.solid(), hx(col), alpha)


def _one(x, y):
    m = np.zeros((N, N), bool)
    if 0 <= x < N and 0 <= y < N:
        m[y, x] = True
    return m


def _norm(x, y, z):
    n = np.sqrt(x * x + y * y + z * z) + 1e-6
    return x / n, y / n, z / n


def bez(p0, p1, p2, p3, n=16):
    t = np.linspace(0, 1, n)[:, None]
    p0, p1, p2, p3 = (np.array(p, np.float32) for p in (p0, p1, p2, p3))
    pts = (1 - t) ** 3 * p0 + 3 * (1 - t) ** 2 * t * p1 + 3 * (1 - t) * t * t * p2 + t ** 3 * p3
    return [tuple(p) for p in pts]


def flat(pts):
    return [c for p in pts for c in p]


def noise(salt=0):
    rng = np.random.default_rng(salt)
    return rng.random((N, N)).astype(np.float32) - 0.5
