package net.saturn.chainmod;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class ChainData {
    private Vec3d startPos;
    private Vec3d endPos;
    private final List<Vec3d> segments;
    private final int segmentCount;
    private boolean followPlayer = false;

    // Physics parameters
    private static final double GRAVITY = 0.5; // Sag factor
    private static final double SEGMENT_STIFFNESS = 0.8;
    private static final int PHYSICS_ITERATIONS = 5;

    public ChainData(Vec3d start, Vec3d end, int segments) {
        this.startPos = start;
        this.endPos = end;
        this.segmentCount = segments;
        this.segments = new ArrayList<>();

        // Initialize segments in a straight line
        initializeSegments();
    }

    private void initializeSegments() {
        segments.clear();
        for (int i = 0; i <= segmentCount; i++) {
            double t = (double) i / segmentCount;
            Vec3d pos = startPos.lerp(endPos, t);
            segments.add(pos);
        }
    }

    public void setFollowPlayer(boolean follow) {
        this.followPlayer = follow;
    }

    public void update(PlayerEntity player) {
        if (followPlayer) {
            // Update end position to follow player
            endPos = player.getEntityPos().add(0, player.getHeight() / 2, 0);
        }

        // Apply catenary curve physics
        applyCatenaryPhysics();

        // Apply collision with player
        applyPlayerCollision(player);

        // Constrain segments to maintain chain length
        constrainSegments();
    }

    private void applyCatenaryPhysics() {
        double distance = startPos.distanceTo(endPos);
        double sag = Math.min(distance * 0.3, GRAVITY * segmentCount * 0.1);

        // Calculate catenary curve
        for (int i = 1; i < segments.size() - 1; i++) {
            double t = (double) i / segmentCount;

            // Linear interpolation between start and end
            Vec3d basePos = startPos.lerp(endPos, t);

            // Apply sag (catenary approximation using parabola)
            double sagAmount = sag * Math.sin(t * Math.PI);
            Vec3d saggedPos = basePos.add(0, -sagAmount, 0);

            // Smooth transition to new position
            Vec3d currentPos = segments.get(i);
            Vec3d newPos = currentPos.lerp(saggedPos, 0.1);
            segments.set(i, newPos);
        }
    }

    private void applyPlayerCollision(PlayerEntity player) {
        Box playerBox = player.getBoundingBox();

        for (int i = 1; i < segments.size() - 1; i++) {
            Vec3d segPos = segments.get(i);

            // Check if segment is inside player bounding box
            if (playerBox.contains(segPos)) {
                // Push segment to nearest surface of player box
                Vec3d pushed = pushToSurface(segPos, playerBox);
                segments.set(i, pushed);
            }
        }
    }

    private Vec3d pushToSurface(Vec3d point, Box box) {
        double minX = box.minX;
        double maxX = box.maxX;
        double minY = box.minY;
        double maxY = box.maxY;
        double minZ = box.minZ;
        double maxZ = box.maxZ;

        // Calculate distances to each face
        double distToMinX = Math.abs(point.x - minX);
        double distToMaxX = Math.abs(point.x - maxX);
        double distToMinY = Math.abs(point.y - minY);
        double distToMaxY = Math.abs(point.y - maxY);
        double distToMinZ = Math.abs(point.z - minZ);
        double distToMaxZ = Math.abs(point.z - maxZ);

        // Find closest face
        double minDist = Math.min(Math.min(Math.min(distToMinX, distToMaxX),
                        Math.min(distToMinY, distToMaxY)),
                Math.min(distToMinZ, distToMaxZ));

        // Push to that face
        if (minDist == distToMinX) return new Vec3d(minX - 0.1, point.y, point.z);
        if (minDist == distToMaxX) return new Vec3d(maxX + 0.1, point.y, point.z);
        if (minDist == distToMinY) return new Vec3d(point.x, minY - 0.1, point.z);
        if (minDist == distToMaxY) return new Vec3d(point.x, maxY + 0.1, point.z);
        if (minDist == distToMinZ) return new Vec3d(point.x, point.y, minZ - 0.1);
        return new Vec3d(point.x, point.y, maxZ + 0.1);
    }

    private void constrainSegments() {
        // Keep start and end fixed
        segments.set(0, startPos);
        segments.set(segments.size() - 1, endPos);

        // Apply constraint iterations to maintain segment lengths
        double targetLength = startPos.distanceTo(endPos) / segmentCount;

        for (int iter = 0; iter < PHYSICS_ITERATIONS; iter++) {
            for (int i = 1; i < segments.size(); i++) {
                Vec3d current = segments.get(i);
                Vec3d previous = segments.get(i - 1);

                Vec3d delta = current.subtract(previous);
                double distance = delta.length();

                if (distance > 0) {
                    double difference = (distance - targetLength) / distance;
                    Vec3d offset = delta.multiply(0.5 * difference * SEGMENT_STIFFNESS);

                    // Don't move the fixed endpoints
                    if (i > 1) {
                        segments.set(i - 1, previous.add(offset));
                    }
                    if (i < segments.size() - 1) {
                        segments.set(i, current.subtract(offset));
                    }
                }
            }
        }
    }

    public List<Vec3d> getSegments() {
        return new ArrayList<>(segments);
    }

    public Vec3d getStartPos() {
        return startPos;
    }

    public Vec3d getEndPos() {
        return endPos;
    }
}