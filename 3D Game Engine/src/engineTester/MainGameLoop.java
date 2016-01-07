package engineTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import models.RawModel;
import models.TexturedModel;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import renderEngine.OBJLoader;
import terrains.Terrain;
import textures.ModelTexture;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import water.WaterFrameBuffers;
import water.WaterRenderer;
import water.WaterShader;
import water.WaterTile;
import entities.Camera;
import entities.Entity;
import entities.Light;
import entities.Player;
import guis.GuiRenderer;
import guis.GuiTexture;

public class MainGameLoop {

	public static void main(String[] args) {

		DisplayManager.createDisplay();
		Loader loader = new Loader();

		// *********TERRAIN TEXTURE STUFF***********

		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grassy"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("textures/dirt"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("textures/pinkFlowers"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("path"));

		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMap"));

		Terrain terrain = new Terrain(0, -1, loader, texturePack, blendMap, "heightmap");

		List<Terrain> terrains = new ArrayList<Terrain>();
		terrains.add(terrain);

		// *****************************************

		RawModel model = OBJLoader.loadObjModel("tree", loader);

		TexturedModel tree = new TexturedModel(model, new ModelTexture(loader.loadTexture("textures/tree")));
		TexturedModel grass = new TexturedModel(OBJLoader.loadObjModel("grassModel", loader),
				new ModelTexture(loader.loadTexture("textures/grassTexture")));
		TexturedModel flower = new TexturedModel(OBJLoader.loadObjModel("grassModel", loader),
				new ModelTexture(loader.loadTexture("textures/flower")));

		ModelTexture fernTexture = new ModelTexture(loader.loadTexture("fern"));
		fernTexture.setNumberOfRows(2);
		TexturedModel fern = new TexturedModel(OBJLoader.loadObjModel("fern", loader), fernTexture);

		grass.getTexture().setHasTransparency(true);
		grass.getTexture().setUseFakeLighting(true);
		flower.getTexture().setHasTransparency(true);
		flower.getTexture().setUseFakeLighting(true);
		fern.getTexture().setHasTransparency(true);

		List<Entity> entities = new ArrayList<Entity>();
		entities.add(new Entity(fern, 2, new Vector3f(100, -5, -100), 0, 100, 0, 0.9f));
		entities.add(new Entity(tree, 2, new Vector3f(70, 0, -50), 0, 100, 0, 5f));
		entities.add(new Entity(tree, 2, new Vector3f(60, 0, -10), 0, 100, 0, 6f));
		entities.add(new Entity(tree, 2, new Vector3f(80, 0, -60), 0, 100, 0, 7f));
		List<Light> lights = new ArrayList<Light>();
		Light sun = new Light(new Vector3f(10000, 10000, -10000), new Vector3f(1.3f, 1.3f, 1.3f));
		lights.add(sun);

		MasterRenderer renderer = new MasterRenderer(loader);

		RawModel playerModel = OBJLoader.loadObjModel("player", loader);
		TexturedModel playerTexturedModel = new TexturedModel(playerModel,
				new ModelTexture(loader.loadTexture("playerTexture")));

		Player player = new Player(playerTexturedModel, new Vector3f(0, 0, 0), 0, 180, 0, 0.6f);
		Camera camera = new Camera(player);

		List<GuiTexture> guiTextures = new ArrayList<GuiTexture>();

		GuiRenderer guiRenderer = new GuiRenderer(loader);
		WaterFrameBuffers buffers = new WaterFrameBuffers();
		
		WaterShader waterShader = new WaterShader();
		WaterRenderer waterRenderer = new WaterRenderer(loader, waterShader, renderer.getProjectionMatrix(), buffers);
		List<WaterTile> waters = new ArrayList<WaterTile>();
		WaterTile water = new WaterTile(75, -75, 0);
		waters.add(water);
		
		while (!Display.isCloseRequested()) {
			camera.move();
			player.move(terrain);
			GL11.glEnable(GL30.GL_CLIP_DISTANCE0);

			// bind to vertial frame buffer
			buffers.bindReflectionFrameBuffer();
			float distance = 2 * (camera.getPosition().y -water.getHeight());
			camera.getPosition().y -= distance;
			camera.invertPitch();
			renderer.renderScene(entities, terrains, lights, camera, new Vector4f(0, 1, 0, -water.getHeight()));
			camera.getPitch();
			buffers.bindRefractionFrameBuffer();
			camera.getPosition().y += distance;
			camera.invertPitch();
			
			renderer.renderScene(entities, terrains, lights, camera, new Vector4f(0, -1, 0, water.getHeight()));

			// unbind to render scene on display
			buffers.unbindCurrentFrameBuffer();

			// render scene to see shit
			GL11.glDisable(GL30.GL_CLIP_DISTANCE0);
			renderer.renderScene(entities, terrains, lights, camera, new Vector4f(0, -1, 0, 15));
			
			waterRenderer.render(waters, camera, sun);
			guiRenderer.render(guiTextures);
			
			DisplayManager.updateDisplay();
		}

		buffers.cleanUp();
		guiRenderer.cleanUp();
		renderer.cleanUp();
		loader.cleanUp();
		DisplayManager.closeDisplay();

	}
}