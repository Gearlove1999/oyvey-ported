package me.alpha432.oyvey.features.modules.combat;

// Importe basierend auf OyVey Konventionen und Minecraft Klassen.
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.setting.Setting;
import me.alpha432.oyvey.util.MathUtil; // Platzhalter: Muss in Ihrem Client existieren
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity; 
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.Comparator;
import java.util.stream.Stream;

public final class AimAssist extends Module { 
    
	// Einstellungen (Settings) im OyVey-Stil
	private final Setting<Float> range = register(new Setting<>("Range", 4.5f, 1.0f, 6.0f));
	private final Setting<Integer> rotationSpeed = register(new Setting<>("Speed", 600, 10, 3600)); 
	private final Setting<Integer> fov = register(new Setting<>("FOV", 120, 30, 360));
    // Höhe, auf die gezielt wird (ersetzt Wurst's AimAtSetting)
    private final Setting<Float> aimPointHeight = register(new Setting<>("AimHeight", 1.5f, 0.0f, 2.0f)); 
	private final Setting<Boolean> checkLOS = register(new Setting<>("CheckLOS", true));
	private final Setting<Boolean> aimWhileBlocking = register(new Setting<>("AimBlock", false));
    
    // Vereinfachte Entity-Filter
	private final Setting<Boolean> targetPlayers = register(new Setting<>("TargetPlayers", true));
	private final Setting<Boolean> targetHostiles = register(new Setting<>("TargetHostiles", true));
	private final Setting<Boolean> targetPassive = register(new Setting<>("TargetPassive", false));
	
	private Entity target;
	private float nextYaw;
	private float nextPitch;
	
	public AimAssist() {
		// Konstruktor-Signatur wie in Ihrem Criticals-Code
		super("AimAssist", "Aids in aiming at entities.", Category.COMBAT, true, false, false); 
	}
	
	@Override
	public void onTick() {
		if(mc.player == null || mc.level == null) return;
		
		target = null;
		
		// Nicht zielen, wenn ein Inventar/Container offen ist
		if(mc.screen instanceof AbstractContainerScreen)
			return;
		
		// Nicht zielen, wenn der Spieler blockt (falls eingestellt)
		if(!aimWhileBlocking.getValue() && mc.player.isUsingItem())
			return;
		
		chooseTarget();
		if(target == null)
			return;
		
		// Zielpunkt berechnen (Mitte der Hitbox + eingestellte Höhe)
		Vec3 hitVec = target.getBoundingBox().getCenter().add(0, aimPointHeight.getValue() - target.getBbHeight() / 2, 0); 
        
		// Line of Sight (LOS) prüfen
		if(checkLOS.getValue()) {
            // Dies ist ein Standard Minecraft Raycast-Check
            if (!mc.level.clip(new net.minecraft.world.level.ClipContext(mc.player.getEyePosition(1.0f), hitVec, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player)).getType().equals(net.minecraft.world.phys.HitResult.Type.MISS))
            {
                target = null;
                return;
            }
		}
		
		// *OYVEY ANPASSUNG*: Rotation berechnen
        // MathUtil.getRotations(Vec3) ist der wahrscheinliche Ersatz
		float[] rotations = MathUtil.getRotations(hitVec); 
		
		float neededYaw = rotations[0];
        float neededPitch = rotations[1];
        
        // Geschwindigkeit berechnen (Geschw./20 Ticks/Sek)
        float speed = rotationSpeed.getValue() / 20f;
        
        // *OYVEY ANPASSUNG*: Sanft zur Rotation drehen (Wurst's slowlyTurnTowards)
        // Dies sind Platzhalter. Sie benötigen diese Logik in Ihrem MathUtil.
        nextYaw = MathUtil.changeAngle(mc.player.getYRot(), neededYaw, speed);
        nextPitch = MathUtil.changeAngle(mc.player.getXRot(), neededPitch, speed); 
        
        // *OYVEY ANPASSUNG*: Rotation anwenden
        // Entweder über einen RotationManager (bevorzugt) oder direkt auf den Spieler
        mc.player.setYRot(nextYaw);
        mc.player.setXRot(nextPitch);
	}
	
	private void chooseTarget() {
		Stream<Entity> stream = mc.level.entities().stream(); 
		
		double rangeSq = range.getValue() * range.getValue();
        
		stream = stream.filter(e -> e != mc.player 
                                 && e instanceof LivingEntity 
                                 && mc.player.distanceToSqr(e) <= rangeSq);
		
        // FOV-Filter
		if(fov.getValue() < 360.0) {
            // *OYVEY ANPASSUNG*: MathUtil.getAngleToLookVec ist der Platzhalter
			stream = stream.filter(e -> MathUtil.getAngleToLookVec(e.getBoundingBox().getCenter()) <= fov.getValue() / 2.0);
		}
		
        // Entity-Filter anwenden
		stream = stream.filter(e -> {
            if (e.isDeadOrDying()) return false;
            
            if (e instanceof Player) {
                return targetPlayers.getValue();
            }
            
            if (e instanceof net.minecraft.world.entity.monster.Monster) {
                return targetHostiles.getValue();
            }
            
            return targetPassive.getValue(); 
		});
		
        // Bestes Ziel wählen (kleinster Winkel zum Fadenkreuz)
		target = stream
			.min(Comparator.comparingDouble(
				e -> MathUtil.getAngleToLookVec(e.getBoundingBox().getCenter()))) 
			.orElse(null);
	}
    
    @Override
    public String getDisplayInfo() {
        return target != null ? target.getName().getString() : null;
    }
}
