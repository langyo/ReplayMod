package eu.crushedpixel.replaymod.events;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import eu.crushedpixel.replaymod.gui.GuiCancelRender;
import eu.crushedpixel.replaymod.gui.elements.GuiMouseInput;
import eu.crushedpixel.replaymod.reflection.MCPNames;
import eu.crushedpixel.replaymod.replay.ReplayHandler;
import eu.crushedpixel.replaymod.replay.ReplayProcess;
import eu.crushedpixel.replaymod.video.ReplayScreenshot;

public class TickAndRenderListener {

	private static Minecraft mc = Minecraft.getMinecraft();
	
	private double lastX, lastY, lastZ;
	private float lastPitch, lastYaw;
	
	private static Field isGamePaused;

	static {
		try {
			isGamePaused = Minecraft.class.getDeclaredField(MCPNames.field("field_71445_n"));
			isGamePaused.setAccessible(true);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@SubscribeEvent
	public void onRenderWorld(RenderWorldLastEvent event) throws
			InvocationTargetException, IOException, IllegalAccessException, IllegalArgumentException {
		if(!ReplayHandler.isInReplay()) return;
		if(ReplayHandler.isInPath()) ReplayProcess.unblockAndTick(false);
		if(ReplayHandler.isCamera()) ReplayHandler.setCameraEntity(ReplayHandler.getCameraEntity());
		if(ReplayHandler.isInReplay() && ReplayHandler.isPaused()) {
			if(mc != null && mc.thePlayer != null)
				MinecraftTicker.runMouseKeyboardTick(mc);
		}
		if((mc.getRenderViewEntity() == mc.thePlayer || !mc.getRenderViewEntity().isEntityAlive())
				&& ReplayHandler.getCameraEntity() != null && !ReplayHandler.isInPath()) {
			ReplayHandler.spectateCamera();
		} else if(!ReplayHandler.isCamera()) {
			lastX = mc.getRenderViewEntity().posX;
			lastY = mc.getRenderViewEntity().posY;
			lastZ = mc.getRenderViewEntity().posZ;
			lastPitch = mc.getRenderViewEntity().rotationPitch;
			lastYaw = mc.getRenderViewEntity().rotationYaw;
		}
		/*
		if(requestScreenshot) {
			requestScreenshot = false;
			ReplayScreenshot.saveScreenshot(mc.getFramebuffer());
		}
		*/
		if(mc.isGamePaused() && ReplayHandler.isInPath()) {
			isGamePaused.set(mc, false);
		}
	}
	
	//private boolean f1Down = false;
	
	@SubscribeEvent
	public void tick(TickEvent event) {
		if(!ReplayHandler.isInReplay()) return;
		
		/*
		if(Keyboard.getEventKeyState() && Keyboard.isKeyDown(Keyboard.KEY_F1) 
				&& ReplayHandler.isInPath() && !ReplayProcess.isVideoRecording() 
				&& mc.currentScreen instanceof GuiMouseInput && !f1Down) {
			mc.gameSettings.hideGUI = !mc.gameSettings.hideGUI;
		}
		
		f1Down = Keyboard.isKeyDown(Keyboard.KEY_F1) && Keyboard.getEventKeyState();
		*/
		
		if(ReplayHandler.getCameraEntity() != null)
			ReplayHandler.getCameraEntity().updateMovement();
		if(ReplayHandler.isInPath()) {
			ReplayProcess.unblockAndTick(true);
			if(ReplayProcess.isVideoRecording() && 
					!(mc.currentScreen instanceof GuiMouseInput || mc.currentScreen instanceof GuiCancelRender)) {
				mc.displayGuiScreen(new GuiMouseInput());
			}
		}
		else onMouseMove(new MouseEvent());
		FMLCommonHandler.instance().bus().post(new InputEvent.KeyInputEvent());
	}
	
	@SubscribeEvent
	public void onMouseMove(MouseEvent event) {
		if(!ReplayHandler.isInReplay()) return;
		boolean flag = Display.isActive();
		flag = true;

		mc.mcProfiler.startSection("mouse");

		if (flag && Minecraft.isRunningOnMac && mc.inGameHasFocus && !Mouse.isInsideWindow())
		{
			Mouse.setGrabbed(false);
			Mouse.setCursorPosition(Display.getWidth() / 2, Display.getHeight() / 2);
			Mouse.setGrabbed(true);
		}

		if (mc.inGameHasFocus && flag && !(ReplayHandler.isInPath()))
		{
			mc.mouseHelper.mouseXYChange();
			float f1 = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
			float f2 = f1 * f1 * f1 * 8.0F;
			float f3 = (float)mc.mouseHelper.deltaX * f2;
			float f4 = (float)mc.mouseHelper.deltaY * f2;
			byte b0 = 1;

			if (mc.gameSettings.invertMouse)
			{
				b0 = -1;
			}

			if(ReplayHandler.getCameraEntity() != null) {
				ReplayHandler.getCameraEntity().setAngles(f3, f4 * (float)b0);
			}
		}
	}
}