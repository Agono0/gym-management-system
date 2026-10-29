package io.m3fx.controls.effect;

import io.m3fx.controls.internal.ThemeColors;
import io.m3fx.core.theme.ColorRole;
import javafx.animation.AnimationTimer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * High-performance kinetic ambient particle network reacting to cursor proximity
 * and emitting shockwave spring ripples on click.
 */
public class M3ParticleCanvas extends Region {

    private static class Particle {
        double x, y;
        double vx, vy;
        double radius;
        double alpha;
    }

    private static class Shockwave {
        double x, y;
        double radius = 0;
        double maxRadius = 160;
        double life = 1.0;
    }

    private final IntegerProperty particleCount = new SimpleIntegerProperty(this, "particleCount", 48);
    private final BooleanProperty interactive = new SimpleBooleanProperty(this, "interactive", true);

    /** Upper bound on live particles — keeps the O(n^2) connection pass bounded. */
    private static final int MAX_PARTICLES = 2000;

    private final Canvas canvas = new Canvas();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Shockwave> shockwaves = new ArrayList<>();
    private final Random random = new Random(42);

    // Cached per attach; ThemeColors must never be resolved per animation frame.
    private Color cachedPrimary;

    private double mouseX = -1000;
    private double mouseY = -1000;
    private boolean isMouseIn = false;

    private AnimationTimer loop;

    /** Creates an ambient particle canvas. */
    public M3ParticleCanvas() {
        getStyleClass().add("m3-particle-canvas");
        getChildren().add(canvas);

        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        widthProperty().addListener((obs, o, n) -> respawnParticles());
        heightProperty().addListener((obs, o, n) -> respawnParticles());

        initGestures();

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            cachedPrimary = null;
            updateLoopState();
        });
        visibleProperty().addListener(e -> updateLoopState());
        disabledProperty().addListener(e -> updateLoopState());

        updateLoopState();

        setPrefSize(400, 240);
        setMinSize(100, 100);
    }

    private void initGestures() {
        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            if (!isInteractive()) return;
            mouseX = e.getX();
            mouseY = e.getY();
            isMouseIn = true;
        });

        canvas.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            isMouseIn = false;
            mouseX = -1000;
            mouseY = -1000;
        });

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (!isInteractive()) return;
            Shockwave sw = new Shockwave();
            sw.x = e.getX();
            sw.y = e.getY();
            shockwaves.add(sw);
        });
    }

    private void respawnParticles() {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) return;

        particles.clear();
        int count = Math.max(0, Math.min(getParticleCount(), MAX_PARTICLES));
        for (int i = 0; i < count; i++) {
            Particle p = new Particle();
            p.x = random.nextDouble() * w;
            p.y = random.nextDouble() * h;
            p.vx = (random.nextDouble() - 0.5) * 28.0;
            p.vy = (random.nextDouble() - 0.5) * 28.0;
            p.radius = 2.0 + random.nextDouble() * 2.5;
            p.alpha = 0.35 + random.nextDouble() * 0.55;
            particles.add(p);
        }
    }

    /** True when the simulation should currently be stepping. */
    private boolean shouldRun() {
        return getScene() != null && isVisible() && !isDisable();
    }

    private void updateLoopState() {
        if (shouldRun()) {
            if (loop == null) {
                loop = new AnimationTimer() {
                    private long last = 0;
                    @Override
                    public void handle(long now) {
                        if (last > 0) {
                            double dt = (now - last) / 1_000_000_000.0;
                            if (dt > 0.1) dt = 0.1;
                            updateAndRender(dt);
                        }
                        last = now;
                    }
                };
            }
            loop.start();
        } else if (loop != null) {
            loop.stop();
        }
    }

    private void updateAndRender(double dt) {
        double w = getWidth() > 0 ? getWidth() : getPrefWidth();
        double h = getHeight() > 0 ? getHeight() : getPrefHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        if (particles.isEmpty()) {
            respawnParticles();
        }

        if (cachedPrimary == null) {
            cachedPrimary = ThemeColors.resolve(this, ColorRole.PRIMARY);
        }
        Color primary = cachedPrimary;
        double maxConnDist = 85.0;

        // Update shockwaves
        for (int i = shockwaves.size() - 1; i >= 0; i--) {
            Shockwave sw = shockwaves.get(i);
            sw.radius += dt * 180.0;
            sw.life -= dt * 1.2;

            if (sw.life <= 0 || sw.radius >= sw.maxRadius) {
                shockwaves.remove(i);
            } else {
                gc.setStroke(Color.color(primary.getRed(), primary.getGreen(), primary.getBlue(), sw.life * 0.4));
                gc.setLineWidth(2.0);
                gc.strokeOval(sw.x - sw.radius, sw.y - sw.radius, sw.radius * 2, sw.radius * 2);
            }
        }

        // Update & Render Particles
        int n = particles.size();
        for (int i = 0; i < n; i++) {
            Particle p = particles.get(i);

            // Drift
            p.x += p.vx * dt;
            p.y += p.vy * dt;

            // Bounce on borders
            if (p.x < 0) { p.x = 0; p.vx = -p.vx; }
            if (p.x > w) { p.x = w; p.vx = -p.vx; }
            if (p.y < 0) { p.y = 0; p.vy = -p.vy; }
            if (p.y > h) { p.y = h; p.vy = -p.vy; }

            // Mouse Repulsion
            if (isMouseIn) {
                double dx = p.x - mouseX;
                double dy = p.y - mouseY;
                double d = Math.sqrt(dx * dx + dy * dy);
                if (d > 0.1 && d < 90.0) {
                    double force = (1.0 - d / 90.0) * 120.0 * dt;
                    p.x += (dx / d) * force;
                    p.y += (dy / d) * force;
                }
            }

            // Shockwave impulse
            for (Shockwave sw : shockwaves) {
                double dx = p.x - sw.x;
                double dy = p.y - sw.y;
                double d = Math.sqrt(dx * dx + dy * dy);
                double diff = Math.abs(d - sw.radius);
                if (diff < 20.0 && d > 0.1) {
                    double push = (1.0 - diff / 20.0) * 180.0 * dt * sw.life;
                    p.x += (dx / d) * push;
                    p.y += (dy / d) * push;
                }
            }

            // Draw particle
            gc.setFill(Color.color(primary.getRed(), primary.getGreen(), primary.getBlue(), p.alpha));
            gc.fillOval(p.x - p.radius, p.y - p.radius, p.radius * 2, p.radius * 2);

            // Connect nearby particles
            for (int j = i + 1; j < n; j++) {
                Particle p2 = particles.get(j);
                double dx = p.x - p2.x;
                double dy = p.y - p2.y;
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < maxConnDist) {
                    double alpha = (1.0 - dist / maxConnDist) * 0.28;
                    gc.setStroke(Color.color(primary.getRed(), primary.getGreen(), primary.getBlue(), alpha));
                    gc.setLineWidth(1.0);
                    gc.strokeLine(p.x, p.y, p2.x, p2.y);
                }
            }
        }
    }

    public IntegerProperty particleCountProperty() {
        return particleCount;
    }

    public int getParticleCount() {
        return particleCount.get();
    }

    public void setParticleCount(int count) {
        this.particleCount.set(count);
        respawnParticles();
    }

    public BooleanProperty interactiveProperty() {
        return interactive;
    }

    public boolean isInteractive() {
        return interactive.get();
    }

    public void setInteractive(boolean interactive) {
        this.interactive.set(interactive);
    }
}

