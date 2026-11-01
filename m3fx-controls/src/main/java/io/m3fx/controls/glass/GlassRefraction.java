package io.m3fx.controls.glass;

import javafx.scene.effect.FloatMap;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * The Liquid Glass fragment shader, ported line-for-line from the liquidGL engine
 * ({@code liquidGL-main/scripts/liquidGL.js}, main {@code fs}) into a per-pixel Java function.
 * Rendering runs over the cached backdrop capture — one pass per invalidation, not per frame —
 * so the exact shader semantics survive on JavaFX without custom GPU code:
 *
 * <pre>
 *   d        = -udRoundBox(p, b, r)                                   signed distance, px
 *   edge     = 1 - smoothstep(0, bevelWidth*min(boxW,boxH), d)         1 at rim, 0 inward
 *   offset   = edge*refraction + edge^10 * bevelDepth                  UV units
 *   centre   = smoothstep(0.15, 0.45, |p| / boxH)                      flat centre, live rim
 *   dir      = cornerNormal(p, b, r, normalize(p+eps))                 rim-aware outward normal
 *   uv       = mapped + dir*offset*centre                              refracted sample
 *   frost    = 16 random taps in a frost*4-texel radius (else a 5-tap cross)
 *   aberr    = R sampled at uv-chroma, B at uv+chroma, chroma = offset*aberration
 * </pre>
 * with the liquidGL defaults {@code refraction = 0.01}, {@code bevelDepth = 0.08},
 * {@code bevelWidth = 0.15}, {@code magnify = 1}, and the fluid-interaction
 * {@code deformPoint}/{SDF-gradient} pair ported verbatim. The nikdelvin map construction
 * ({@code liquid-glass-main/src/utils/liquidGlass.ts}) is honored as the SVG_DISPLACEMENT
 * profile through the same shading loop with its gradient-screen displacement field.
 *
 * <p>Sources: {@code docs/SPEC_NOTES/liquid-glass.md}; liquidGL engine (user-provided
 * {@code liquidGL-main}); nikdelvin {@code liquid-glass-main} (user-provided); Apple WWDC25
 * "Meet Liquid Glass" (behavior framing). Defaults are the reference engines', not invented.
 */
public final class GlassRefraction {

    private GlassRefraction() {
    }

    /** Fluid-interaction state (liquidGL {@code u_interaction}), pointer in pane UV units. */
    public record Interaction(double pointerX, double pointerY, double deltaX, double deltaY,
                              double radius) {
        /** No interaction: the identity deformation. */
        public static final Interaction NONE = new Interaction(0.5, 0.5, 0.0, 0.0, 0.0);
    }

    /** Shader uniform bundle for one render pass (all values in pane pixels unless noted). */
    public record ShaderParams(double boxW, double boxH, double radius, double refraction,
                               double bevelDepth, double bevelWidth, double frost,
                               double aberration, double magnify, Color tint,
                               Interaction interaction) {
    }

    /** liquidGL defaults (engine {@code defaults} block). */
    public static final double DEFAULT_REFRACTION = 0.01;
    public static final double DEFAULT_BEVEL_DEPTH = 0.08;
    public static final double DEFAULT_BEVEL_WIDTH = 0.15;
    public static final double DEFAULT_MAGNIFY = 1.0;
    /** nikdelvin defaults ({@code LiquidGlass.astro}). */
    public static final double DEFAULT_NIKDELVIN_DEPTH = 10.0;
    public static final double DEFAULT_NIKDELVIN_STRENGTH = 100.0;

    /** A built displacement map plus the per-axis scales that decode it into pixel offsets. */
    public record DisplacementField(FloatMap map, double scaleX, double scaleY) {
    }

    private static final double SDF_EPS = 0.0001;
    private static final int FROST_SAMPLES = 16;

    // ------------------------------------------------------------------
    // liquidGL primitives (exact ports)
    // ------------------------------------------------------------------

    /** Exact port of liquidGL's {@code udRoundBox}: signed distance to a rounded box (px). */
    public static double sdfRoundBox(double px, double py, double halfW, double halfH, double r) {
        double qx = Math.abs(px) - halfW + r;
        double qy = Math.abs(py) - halfH + r;
        double mx = Math.max(qx, 0.0);
        double my = Math.max(qy, 0.0);
        return Math.hypot(mx, my) + Math.min(Math.max(qx, qy), 0.0) - r;
    }

    /**
     * Exact port of liquidGL's {@code cornerNormal}: rim-aware outward normal, blending the
     * fallback radial direction toward the corner direction across the straight segments.
     */
    public static double[] cornerNormal(double px, double py, double halfW, double halfH,
                                        double r, double fallbackX, double fallbackY) {
        double qx = Math.abs(px) - halfW + r;
        double qy = Math.abs(py) - halfH + r;
        double mx = Math.max(qx, 0.0);
        double my = Math.max(qy, 0.0);
        double len = Math.hypot(mx, my);
        if (len <= 0.0) {
            return new double[] {fallbackX, fallbackY};
        }
        double w = smoothstep(0.0, Math.max(r * 0.5, 1.0), Math.min(mx, my));
        if (w <= 0.0) {
            return new double[] {fallbackX, fallbackY};
        }
        double sx = px < 0.0 ? -1.0 : 1.0;
        double sy = py < 0.0 ? -1.0 : 1.0;
        double nx = sx * mx / len;
        double ny = sy * my / len;
        double mixX = fallbackX + (nx - fallbackX) * w;
        double mixY = fallbackY + (ny - fallbackY) * w;
        double mixLen = Math.hypot(mixX, mixY);
        if (mixLen <= SDF_EPS) {
            return new double[] {fallbackX, fallbackY};
        }
        return new double[] {mixX / mixLen, mixY / mixLen};
    }

    /** Standard smoothstep, guarded against a zero-wide edge. */
    public static double smoothstep(double edge0, double edge1, double x) {
        if (edge1 == edge0) {
            return x < edge0 ? 0.0 : 1.0;
        }
        double t = Math.max(0.0, Math.min(1.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    /**
     * Exact port of liquidGL's {@code deformPoint}: pulls surface points with the pointer's
     * viscous displacement, fading with a smoothstep over the interaction radius.
     */
    public static double[] deformPoint(double px, double py, double boxW, double boxH,
                                       Interaction interaction) {
        if (interaction == null || interaction.radius() <= 0.0) {
            return new double[] {px, py};
        }
        double cx = (interaction.pointerX() - 0.5) * boxW;
        double cy = ((1.0 - interaction.pointerY()) - 0.5) * boxH;
        double reach = interaction.radius() * Math.min(boxW, boxH);
        double dist = Math.hypot(px - cx, py - cy);
        double influence = 1.0 - smoothstep(0.0, reach, dist);
        double dx = interaction.deltaX() * boxW * influence;
        double dy = -interaction.deltaY() * boxH * influence;
        return new double[] {px - dx, py - dy};
    }

    /**
     * Exact port of liquidGL's {@code edgeFactor}: 1 at the shape border, easing to 0 at
     * {@code bevelWidth * min(boxW, boxH)} inward.
     */
    public static double edgeFactor(double px, double py, double halfW, double halfH, double r,
                                    double bevelWidth, double boxW, double boxH) {
        double d = -sdfRoundBox(px, py, halfW, halfH, r);
        double bevelPx = bevelWidth * Math.min(boxW, boxH);
        return 1.0 - smoothstep(0.0, bevelPx, d);
    }

    /**
     * The shader's refraction offset at one surface point: the UV displacement
     * {@code dir * offsetAmt * centreBlend} (exactly the shader's {@code offset} vector).
     *
     * @param pPx         deformed point x relative to the box centre (px)
     * @param pPy         deformed point y relative to the box centre (px)
     * @param originalX   undeformed point x (px, for the gradient blend weight)
     * @param originalY   undeformed point y (px)
     * @param boxW        pane width in px
     * @param boxH        pane height in px
     * @param radius      corner radius in px
     * @param refraction  liquidGL {@code u_refraction} (UV units)
     * @param bevelDepth  bevel depth (UV, {@code edge^10} term)
     * @param bevelWidth  bevel width (fraction of the smaller side)
     * @param interaction fluid-interaction state ({@code null} = none)
     * @return {@code {offsetX, offsetY}} in texture UV units
     */
    public static double[] refractionOffset(double pPx, double pPy, double originalX,
                                            double originalY, double boxW, double boxH,
                                            double radius, double refraction, double bevelDepth,
                                            double bevelWidth, Interaction interaction) {
        double halfW = boxW / 2.0;
        double halfH = boxH / 2.0;
        double edge = edgeFactor(pPx, pPy, halfW, halfH, radius, bevelWidth, boxW, boxH);
        double offsetAmt = edge * refraction + Math.pow(edge, 10.0) * bevelDepth;
        double plen = Math.hypot(pPx / boxH, pPy / boxH);
        double centreBlend = smoothstep(0.15, 0.45, plen);
        // Shader fallback: normalize(p + vec2(0.000001)) — the outward radial direction.
        double fbLen = Math.hypot(pPx + 0.000001, pPy);
        double fbX = fbLen <= SDF_EPS ? 0.0 : (pPx + 0.000001) / fbLen;
        double fbY = fbLen <= SDF_EPS ? 0.0 : pPy / fbLen;
        double[] dir = cornerNormal(pPx, pPy, halfW, halfH, radius, fbX, fbY);
        if (interaction != null && interaction.radius() > 0.0) {
            double[] ppx = deformPoint(originalX + 0.5, originalY, boxW, boxH, interaction);
            double[] pmx = deformPoint(originalX - 0.5, originalY, boxW, boxH, interaction);
            double[] ppy = deformPoint(originalX, originalY + 0.5, boxW, boxH, interaction);
            double[] pmy = deformPoint(originalX, originalY - 0.5, boxW, boxH, interaction);
            double gx = sdfRoundBox(ppx[0], ppx[1], halfW, halfH, radius)
                    - sdfRoundBox(pmx[0], pmx[1], halfW, halfH, radius);
            double gy = sdfRoundBox(ppy[0], ppy[1], halfW, halfH, radius)
                    - sdfRoundBox(pmy[0], pmy[1], halfW, halfH, radius);
            if (Math.hypot(gx, gy) > SDF_EPS) {
                double deformDist = Math.min(
                        Math.hypot(pPx - originalX, pPy - originalY), 1.0);
                double mixX = dir[0] + ((gx / Math.hypot(gx, gy)) - dir[0]) * deformDist;
                double mixY = dir[1] + ((gy / Math.hypot(gx, gy)) - dir[1]) * deformDist;
                double mixLen = Math.hypot(mixX, mixY);
                if (mixLen > SDF_EPS) {
                    dir[0] = mixX / mixLen;
                    dir[1] = mixY / mixLen;
                }
            }
        }
        return new double[] {dir[0] * offsetAmt * centreBlend, dir[1] * offsetAmt * centreBlend};
    }

    /**
     * liquidGL's {@code random}: the deterministic hash the frost taps use, ported so renders
     * are reproducible.
     */
    public static double random(double sx, double sy) {
        return Math.abs(Math.sin(sx * 12.9898 + sy * 78.233) * 43758.5453123) % 1.0;
    }

    // ------------------------------------------------------------------
    // The shaded render pass (main() port over the captured backdrop)
    // ------------------------------------------------------------------

    /**
     * Renders the refracted pane into {@code out} from the captured backdrop, per pixel, exactly
     * the liquidGL fragment shader: shape mask, rim refraction with the flat centre, frost,
     * chromatic aberration, magnification, the tint mix and the in-shape alpha mask. Pixels the
     * shader leaves to the shadow pass render transparent (the skin's DropShadow covers them).
     *
     * @param backdrop    the captured (padded) backdrop image
     * @param out         the target image (pane-sized)
     * @param offsetX     capture x of the pane's top-left inside the backdrop
     * @param offsetY     capture y of the pane's top-left inside the backdrop
     * @param params      shader uniforms for this pass
     * @param interaction fluid-interaction state ({@code null} = none)
     */
    public static void render(WritableImage backdrop, WritableImage out,
                              double offsetX, double offsetY, ShaderParams params,
                              Interaction interaction) {
        int outW = (int) out.getWidth();
        int outH = (int) out.getHeight();
        if (backdrop == null || outW <= 0 || outH <= 0) {
            return;
        }
        PixelReader src = backdrop.getPixelReader();
        var writer = out.getPixelWriter();
        double boxW = params.boxW();
        double boxH = params.boxH();
        double halfW = boxW / 2.0;
        double halfH = boxH / 2.0;
        double radius = Math.min(params.radius(), Math.min(halfW, halfH));
        double bevelPx = params.bevelWidth() * Math.min(boxW, boxH);
        Color tint = params.tint();
        double tintA = tint == null ? 0.0 : clamp01(tint.getOpacity());
        double texelX = 1.0 / Math.max(1.0, backdrop.getWidth());
        double texelY = 1.0 / Math.max(1.0, backdrop.getHeight());
        for (int py = 0; py < outH; py++) {
            for (int px = 0; px < outW; px++) {
                // lensUV over the pane; p relative to the box centre (shader: (lensUV-res-subpixel)/box).
                double lensU = (px + 0.5) / outW;
                double lensV = (py + 0.5) / outH;
                double originalX = (lensU - 0.5) * boxW;
                double originalY = (lensV - 0.5) * boxH;
                double[] p = deformPoint(originalX, originalY, boxW, boxH, interaction);
                double dmask = sdfRoundBox(p[0], p[1], halfW, halfH, radius);
                double inShape = 1.0 - smoothstep(-0.5, 0.5, dmask);
                if (inShape <= 0.0) {
                    writer.setColor(px, py, Color.TRANSPARENT);
                    continue;
                }
                double edge = edgeFactor(p[0], p[1], halfW, halfH, radius,
                        params.bevelWidth(), boxW, boxH);
                double offsetAmt = edge * params.refraction()
                        + Math.pow(edge, 10.0) * params.bevelDepth();
                double plen = Math.hypot(p[0] / boxH, p[1] / boxH);
                double centreBlend = smoothstep(0.15, 0.45, plen);
                double fbLen = Math.hypot(p[0] + 0.000001, p[1]);
                double fbX = fbLen <= SDF_EPS ? 0.0 : (p[0] + 0.000001) / fbLen;
                double fbY = fbLen <= SDF_EPS ? 0.0 : p[1] / fbLen;
                double[] dir = cornerNormal(p[0], p[1], halfW, halfH, radius, fbX, fbY);
                if (interaction != null && interaction.radius() > 0.0) {
                    double[] ppx = deformPoint(originalX + 0.5, originalY, boxW, boxH, interaction);
                    double[] pmx = deformPoint(originalX - 0.5, originalY, boxW, boxH, interaction);
                    double[] ppy = deformPoint(originalX, originalY + 0.5, boxW, boxH, interaction);
                    double[] pmy = deformPoint(originalX, originalY - 0.5, boxW, boxH, interaction);
                    double gx = sdfRoundBox(ppx[0], ppx[1], halfW, halfH, radius)
                            - sdfRoundBox(pmx[0], pmx[1], halfW, halfH, radius);
                    double gy = sdfRoundBox(ppy[0], ppy[1], halfW, halfH, radius)
                            - sdfRoundBox(pmy[0], pmy[1], halfW, halfH, radius);
                    if (Math.hypot(gx, gy) > SDF_EPS) {
                        double deformDist = Math.min(
                                Math.hypot(p[0] - originalX, p[1] - originalY), 1.0);
                        double mixX = dir[0] + ((gx / Math.hypot(gx, gy)) - dir[0]) * deformDist;
                        double mixY = dir[1] + ((gy / Math.hypot(gx, gy)) - dir[1]) * deformDist;
                        double mixLen = Math.hypot(mixX, mixY);
                        if (mixLen > SDF_EPS) {
                            dir[0] = mixX / mixLen;
                            dir[1] = mixY / mixLen;
                        }
                    }
                }
                double offU = dir[0] * offsetAmt * centreBlend;
                double offV = dir[1] * offsetAmt * centreBlend;

                // mapped: the pane rect inside the padded capture; magnify zooms localUV.
                double localU = (lensU - 0.5) / Math.max(params.magnify(), SDF_EPS) + 0.5;
                double localV = (lensV - 0.5) / Math.max(params.magnify(), SDF_EPS) + 0.5;
                double mapX = offsetX + localU * boxW;
                double mapY = offsetY + localV * boxH;
                // The shader adds its UV offset to the refracted sample position; the WebGL
                // capture's Y flip cancels out on a JavaFX (top-down) capture, so both axes add.
                double refrX = mapX + offU * boxW;
                double refrY = mapY + offV * boxH;

                Color refrCol;
                if (params.frost() > 0.0) {
                    double frostRadius = params.frost() * 4.0;
                    double rSum = 0.0, gSum = 0.0, bSum = 0.0, aSum = 0.0;
                    double chromaPxX = offU * params.aberration() * boxW;
                    double chromaPxY = offV * params.aberration() * boxH;
                    for (int i = 0; i < FROST_SAMPLES; i++) {
                        double angle = random(lensU + i, lensV) * 2.0 * Math.PI;
                        double dist = Math.sqrt(random(lensU, lensV - i)) * frostRadius;
                        double fX = Math.cos(angle) * dist;
                        double fY = Math.sin(angle) * dist;
                        Color c = sampleClamped(src, backdrop, refrX + fX - chromaPxX,
                                refrY + fY);
                        Color g = sampleClamped(src, backdrop, refrX + fX, refrY + fY);
                        Color b = sampleClamped(src, backdrop, refrX + fX + chromaPxX,
                                refrY + fY);
                        rSum += params.aberration() > 0.0 ? c.getRed() : g.getRed();
                        gSum += g.getGreen();
                        bSum += params.aberration() > 0.0 ? b.getBlue() : g.getBlue();
                        aSum += g.getOpacity();
                    }
                    refrCol = Color.color(rSum / FROST_SAMPLES, gSum / FROST_SAMPLES,
                            bSum / FROST_SAMPLES, aSum / FROST_SAMPLES);
                } else {
                    Color base = sampleClamped(src, backdrop, refrX, refrY);
                    Color r5 = sampleClamped(src, backdrop, refrX + texelX, refrY);
                    Color l5 = sampleClamped(src, backdrop, refrX - texelX, refrY);
                    Color u5 = sampleClamped(src, backdrop, refrX, refrY + texelY);
                    Color d5 = sampleClamped(src, backdrop, refrX, refrY - texelY);
                    refrCol = Color.color(
                            (base.getRed() + r5.getRed() + l5.getRed() + u5.getRed() + d5.getRed()) / 5.0,
                            (base.getGreen() + r5.getGreen() + l5.getGreen() + u5.getGreen() + d5.getGreen()) / 5.0,
                            (base.getBlue() + r5.getBlue() + l5.getBlue() + u5.getBlue() + d5.getBlue()) / 5.0,
                            (base.getOpacity() + r5.getOpacity() + l5.getOpacity() + u5.getOpacity() + d5.getOpacity()) / 5.0);
                    if (params.aberration() > 0.0) {
                        Color rSide = sampleClamped(src, backdrop,
                                refrX - offU * boxW * params.aberration(), refrY);
                        Color bSide = sampleClamped(src, backdrop,
                                refrX + offU * boxW * params.aberration(), refrY);
                        refrCol = Color.color(rSide.getRed(), refrCol.getGreen(), bSide.getBlue(),
                                refrCol.getOpacity());
                    }
                }
                if (refrCol.getOpacity() < 0.1) {
                    refrCol = sampleClamped(src, backdrop, mapX, mapY);
                }
                double r = refrCol.getRed();
                double g = refrCol.getGreen();
                double b = refrCol.getBlue();
                if (tintA > 0.0) {
                    r = r + (r * tint.getRed() - r) * tintA;
                    g = g + (g * tint.getGreen() - g) * tintA;
                    b = b + (b * tint.getBlue() - b) * tintA;
                }
                writer.setColor(px, py, Color.color(
                        clamp01(r) * inShape, clamp01(g) * inShape, clamp01(b) * inShape,
                        clamp01(refrCol.getOpacity()) * inShape));
            }
        }
    }

    /** Clamped bilinear sample of the backdrop capture (shader texture2D semantics). */
    private static Color sampleClamped(PixelReader reader, WritableImage image,
                                       double x, double y) {
        double maxX = image.getWidth() - 1.0;
        double maxY = image.getHeight() - 1.0;
        double cx = Math.max(0.0, Math.min(maxX, x));
        double cy = Math.max(0.0, Math.min(maxY, y));
        int x0 = (int) Math.floor(cx);
        int y0 = (int) Math.floor(cy);
        int x1 = Math.min(x0 + 1, (int) maxX);
        int y1 = Math.min(y0 + 1, (int) maxY);
        double fx = cx - x0;
        double fy = cy - y0;
        Color c00 = reader.getColor(x0, y0);
        Color c10 = reader.getColor(x1, y0);
        Color c01 = reader.getColor(x0, y1);
        Color c11 = reader.getColor(x1, y1);
        return Color.color(
                lerp(lerp(c00.getRed(), c10.getRed(), fx), lerp(c01.getRed(), c11.getRed(), fx), fy),
                lerp(lerp(c00.getGreen(), c10.getGreen(), fx), lerp(c01.getGreen(), c11.getGreen(), fx), fy),
                lerp(lerp(c00.getBlue(), c10.getBlue(), fx), lerp(c01.getBlue(), c11.getBlue(), fx), fy),
                lerp(lerp(c00.getOpacity(), c10.getOpacity(), fx), lerp(c01.getOpacity(), c11.getOpacity(), fx), fy));
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // ------------------------------------------------------------------
    // Nikdelvin SVG-map port (liquid-glass-main/src/utils/liquidGlass.ts)
    // ------------------------------------------------------------------

    /**
     * Builds the nikdelvin displacement field exactly: navy base, screen-blended linear
     * gradients (insets {@code 0.15 * radius} on each axis), and the gray rounded rect inset by
     * {@code depth} whose screen blend neutralizes the centre; the SVG {@code blur(2px)} group
     * and {@code blur(depth)} rect filter are approximated with box-blur passes of radius
     * {@code max(1, round(depth))} and 2. DisplacementMap scale = {@code strength} (their
     * {@code scale} attribute), identical on both axes.
     *
     * @param width   pane width in px
     * @param height  pane height in px
     * @param radius  corner radius in px
     * @param depth   bezel depth in px (gray rect inset + blur radius)
     * @param strength displacement scale in px (their {@code scale} attribute)
     * @return the field with per-axis scales equal to {@code strength}
     */
    public static DisplacementField buildNikdelvinField(double width, double height,
                                                        double radius, double depth,
                                                        double strength) {
        int w = Math.max(1, (int) Math.ceil(width));
        int h = Math.max(1, (int) Math.ceil(height));
        double[][][] channels = nikdelvinChannels(width, height, radius, depth);
        double[][] rMasked = channels[0];
        double[][] gMasked = channels[1];
        FloatMap map = new FloatMap(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                map.setSamples(x, y, (float) rMasked[y][x], (float) gMasked[y][x], 0.0f, 1.0f);
            }
        }
        return new DisplacementField(map, Math.max(1.0, strength), Math.max(1.0, strength));
    }

    /**
     * The raw nikdelvin displacement channels (R = x field, G = y field), values in [0,1].
     * Exposed package-private so tests can assert the construction without reading a FloatMap.
     *
     * @param width  pane width in px
     * @param height pane height in px
     * @param radius corner radius in px
     * @param depth  bezel depth in px (gray rect inset + blur radius)
     * @return {@code {rField, gField}} at pixel resolution
     */
    static double[][][] nikdelvinChannels(double width, double height, double radius,
                                          double depth) {
        int w = Math.max(1, (int) Math.ceil(width));
        int h = Math.max(1, (int) Math.ceil(height));
        double xInset = radius * 0.15;
        double yInset = radius * 0.15;
        double[][] rField = new double[h][w];
        double[][] gField = new double[h][w];
        for (int y = 0; y < h; y++) {
            double gY = 1.0 - clamp01((y + 0.5 - yInset) / Math.max(1.0, height - 2.0 * yInset));
            for (int x = 0; x < w; x++) {
                double rX = 1.0 - clamp01((x + 0.5 - xInset) / Math.max(1.0, width - 2.0 * xInset));
                // Base #000080 (R=0,G=0) screened with the Y gradient (R=0, G=gY) then the X
                // gradient (R=rX, G=0): screen(a,b) = a + b - ab.
                rField[y][x] = rX;
                gField[y][x] = gY;
            }
        }
        // Gray rounded rect inset by `depth`: screen(prev, 128/255) inside, then blurred.
        double halfW = width / 2.0;
        double halfH = height / 2.0;
        double innerHalfW = Math.max(0.0, halfW - depth);
        double innerHalfH = Math.max(0.0, halfH - depth);
        double innerRadius = Math.min(radius, Math.min(innerHalfW, innerHalfH));
        double gray = 128.0 / 255.0;
        double[][] rMasked = new double[h][w];
        double[][] gMasked = new double[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double px = x + 0.5 - halfW;
                double py = y + 0.5 - halfH;
                double d = sdfRoundBox(px, py, innerHalfW, innerHalfH, innerRadius);
                if (d <= 0.0) {
                    rMasked[y][x] = rField[y][x] + gray - rField[y][x] * gray;
                    gMasked[y][x] = gField[y][x] + gray - gField[y][x] * gray;
                } else {
                    rMasked[y][x] = rField[y][x];
                    gMasked[y][x] = gField[y][x];
                }
            }
        }
        // Approximate the SVG blur(2px) group + blur(depth) rect with two box passes.
        int blurRadius = Math.max(1, (int) Math.round(depth));
        rMasked = boxBlur(rMasked, blurRadius);
        gMasked = boxBlur(gMasked, blurRadius);
        rMasked = boxBlur(rMasked, 2);
        gMasked = boxBlur(gMasked, 2);
        return new double[][][] {rMasked, gMasked};
    }

    /** Separable box blur over a per-pixel channel field (approximates the SVG blur chain). */
    private static double[][] boxBlur(double[][] field, int radius) {
        int h = field.length;
        int w = field.length > 0 ? field[0].length : 0;
        if (radius <= 0 || h == 0 || w == 0) {
            return field;
        }
        double[][] tmp = new double[h][w];
        double[][] out = new double[h][w];
        for (int y = 0; y < h; y++) {
            double sum = 0.0;
            for (int x = -radius; x <= radius; x++) {
                sum += field[y][Math.max(0, Math.min(w - 1, x))];
            }
            for (int x = 0; x < w; x++) {
                tmp[y][x] = sum / (2.0 * radius + 1.0);
                int outX = Math.max(0, Math.min(w - 1, x - radius));
                int inX = Math.max(0, Math.min(w - 1, x + radius + 1));
                sum += field[y][inX] - field[y][outX];
            }
        }
        for (int x = 0; x < w; x++) {
            double sum = 0.0;
            for (int y = -radius; y <= radius; y++) {
                sum += tmp[Math.max(0, Math.min(h - 1, y))][x];
            }
            for (int y = 0; y < h; y++) {
                out[y][x] = sum / (2.0 * radius + 1.0);
                int outY = Math.max(0, Math.min(h - 1, y - radius));
                int inY = Math.max(0, Math.min(h - 1, y + radius + 1));
                sum += tmp[inY][x] - tmp[outY][x];
            }
        }
        return out;
    }

    /**
     * Returns the shape's effective corner radius in px, clamped to half the smaller side.
     *
     * @param shape glass silhouette
     * @param cornerRadius corner radius (RECT)
     * @param width pane width in px
     * @param height pane height in px
     * @return effective radius (&ge; 0)
     */
    public static double effectiveRadius(GlassShape shape, double cornerRadius, double width,
                                         double height) {
        double halfMin = Math.max(0.0, Math.min(width, height) / 2.0);
        return switch (shape == null ? GlassShape.CAPSULE : shape) {
            case CAPSULE, CIRCLE -> halfMin;
            case RECT -> Math.max(0.0, Math.min(cornerRadius, halfMin));
        };
    }

    private static double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
