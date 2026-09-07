package com.velorise.cameralockon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Learns small residual bow aim corrections by distance, relative height, and launch pitch. */
public final class AdaptiveAimCalibration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_SAMPLES = 160;
    private static final double MAX_CORRECTION = 0.35D;
    private static final Map<Integer, Flight> FLIGHTS = new HashMap<>();
    private static final List<Sample> SAMPLES = new ArrayList<>();
    private static boolean loaded;
    private static int saveDelay;

    private AdaptiveAimCalibration() {}

    public static void tick(Minecraft minecraft, LocalPlayer player) {
        ensureLoaded(minecraft);
        if (!CameraLockOnConfig.ADAPTIVE_AIM_CALIBRATION.get() || player.level() == null) return;
        LivingEntity target = LockOnController.isActive() ? LockOnController.getLockedTarget() : null;
        AABB scan = player.getBoundingBox().inflate(96.0D);
        for (AbstractArrow arrow : player.level().getEntitiesOfClass(AbstractArrow.class, scan,
                a -> a.getOwner() == player && !FLIGHTS.containsKey(a.getId()))) {
            if (target == null || !target.isAlive()) continue;
            Vec3 motion = new Vec3(target.getX()-target.xo, target.getY()-target.yo, target.getZ()-target.zo);
            if (motion.lengthSqr() > 0.0025D) continue;
            Vec3 aim = ProjectileAimCalculator.getLastResolvedAimPoint(target);
            if (aim == null) aim = target.position().add(0, target.getBbHeight()*0.55D, 0);
            Vec3 eye = player.getEyePosition();
            double distance = Math.hypot(aim.x-eye.x, aim.z-eye.z);
            double height = aim.y-eye.y;
            Vec3 velocity = arrow.getDeltaMovement();
            double pitch = Math.toDegrees(Math.atan2(velocity.y, Math.hypot(velocity.x, velocity.z)));
            FLIGHTS.put(arrow.getId(), new Flight(arrow, target.getId(), aim, eye, distance, height, pitch));
        }

        Iterator<Flight> it = FLIGHTS.values().iterator();
        while (it.hasNext()) {
            Flight f = it.next();
            Entity e = player.level().getEntity(f.arrowId);
            LivingEntity t = entityAsLiving(player, f.targetId);
            if (!(e instanceof AbstractArrow arrow) || t == null || !t.isAlive()) { finish(f); it.remove(); continue; }
            f.age++;
            Vec3 p = arrow.position();
            double d = p.distanceToSqr(f.aimPoint);
            if (d < f.closestDistanceSq) { f.closestDistanceSq=d; f.closestPoint=p; }
            if (f.age > 100 || passedTarget(f, p)) { finish(f); it.remove(); }
        }
        if (saveDelay > 0 && --saveDelay == 0) save(minecraft);
    }

    public static Vec3 apply(Vec3 aimPoint, Vec3 eye) {
        if (!CameraLockOnConfig.ADAPTIVE_AIM_CALIBRATION.get() || SAMPLES.size() < 3) return aimPoint;
        Vec3 delta=aimPoint.subtract(eye); double distance=Math.hypot(delta.x,delta.z), height=delta.y;
        double pitch=Math.toDegrees(Math.atan2(delta.y, Math.max(1.0E-6D, Math.hypot(delta.x,delta.z))));
        Vec3 forward=new Vec3(delta.x,0,delta.z); if (forward.lengthSqr()<1e-8) return aimPoint; forward=forward.normalize();
        Vec3 right=new Vec3(-forward.z,0,forward.x);
        double sum=0, side=0, up=0;
        for (Sample s:SAMPLES) {
            double metric=Math.abs(s.distance-distance)*0.09D+Math.abs(s.height-height)*0.22D+Math.abs(s.pitch-pitch)*0.045D;
            double w=1.0D/(1.0D+metric*metric); sum+=w; side+=s.side*w; up+=s.up*w;
        }
        if (sum<1.0D) return aimPoint;
        side=clamp(side/sum,-MAX_CORRECTION,MAX_CORRECTION); up=clamp(up/sum,-MAX_CORRECTION,MAX_CORRECTION);
        return aimPoint.add(right.scale(side)).add(0,up,0);
    }

    public static void reset(Minecraft minecraft) { SAMPLES.clear(); FLIGHTS.clear(); save(minecraft); }
    public static int sampleCount() { return SAMPLES.size(); }

    private static void finish(Flight f) {
        if (f.closestPoint==null || f.closestDistanceSq>2.25D || f.age<3) return;
        Vec3 to=f.aimPoint.subtract(f.closestPoint);
        Vec3 horizontal=new Vec3(f.aimPoint.x-f.eye.x,0,f.aimPoint.z-f.eye.z);
        if (horizontal.lengthSqr()<1e-8) return;
        Vec3 right=new Vec3(-horizontal.z,0,horizontal.x).normalize();
        double side=clamp(to.dot(right),-MAX_CORRECTION,MAX_CORRECTION);
        double up=clamp(to.y,-MAX_CORRECTION,MAX_CORRECTION);
        if (Math.abs(side)<0.01D && Math.abs(up)<0.01D) return;
        SAMPLES.add(new Sample(f.distance,f.height,f.pitch,side,up));
        while(SAMPLES.size()>MAX_SAMPLES) SAMPLES.remove(0);
        saveDelay=40;
    }
    private static boolean passedTarget(Flight f, Vec3 p) { Vec3 a=f.aimPoint.subtract(f.eye), b=p.subtract(f.eye); return b.dot(a)>a.lengthSqr()+4.0D; }
    private static LivingEntity entityAsLiving(LocalPlayer p,int id){ Entity e=p.level().getEntity(id); return e instanceof LivingEntity l?l:null; }
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    private static Path file(Minecraft m){return m.gameDirectory.toPath().resolve("config").resolve("camera_lockon-adaptive-calibration.json");}
    private static void ensureLoaded(Minecraft m){if(loaded)return;loaded=true;Path p=file(m);if(!Files.exists(p))return;try(Reader r=Files.newBufferedReader(p)){Store s=GSON.fromJson(r,Store.class);if(s!=null&&s.samples!=null)SAMPLES.addAll(s.samples);}catch(Exception ignored){}}
    private static void save(Minecraft m){try{Path p=file(m);Files.createDirectories(p.getParent());try(Writer w=Files.newBufferedWriter(p)){GSON.toJson(new Store(SAMPLES),w);}}catch(Exception e){System.err.println("[Camera Lock-On] Could not save adaptive calibration: "+e.getMessage());}}

    private static final class Flight { final int arrowId,targetId; final Vec3 aimPoint,eye; final double distance,height,pitch; int age; double closestDistanceSq=Double.MAX_VALUE; Vec3 closestPoint; Flight(AbstractArrow a,int t,Vec3 aim,Vec3 eye,double d,double h,double p){arrowId=a.getId();targetId=t;aimPoint=aim;this.eye=eye;distance=d;height=h;pitch=p;} }
    private static final class Sample { double distance,height,pitch,side,up; Sample(){} Sample(double d,double h,double p,double s,double u){distance=d;height=h;pitch=p;side=s;up=u;} }
    private static final class Store { List<Sample> samples; Store(List<Sample> s){samples=new ArrayList<>(s);} }
}
