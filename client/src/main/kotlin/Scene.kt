import org.w3c.dom.HTMLCanvasElement
import org.khronos.webgl.WebGLRenderingContext as GL //# GL# we need this for the constants declared ˙HUN˙ a constansok miatt kell
import kotlin.js.Date
import vision.gears.webglmath.UniformProvider
import vision.gears.webglmath.Vec1
import vision.gears.webglmath.Vec2
import vision.gears.webglmath.Vec3
import vision.gears.webglmath.Vec4
import vision.gears.webglmath.Mat4
import kotlin.math.*

class Scene (
  val gl : WebGL2RenderingContext)  : UniformProvider("scene") {

  val vsTextured = Shader(gl, GL.VERTEX_SHADER, "textured-vs.glsl")
  val vsBackground = Shader(gl, GL.VERTEX_SHADER, "background-vs.glsl")  
  val fsTextured = Shader(gl, GL.FRAGMENT_SHADER, "textured-fs.glsl")
  val texturedProgram = Program(gl, vsTextured, fsTextured)
  val backgroundProgram = Program(gl, vsBackground, fsTextured)

  //TODO: create various materials with different solidColor settings
  val fighterMaterial = Material(texturedProgram).apply{
    this["colorTexture"]?.set(Texture2D(gl, "media/fighter.png"))
  }
  val thrusterMaterial = Material(texturedProgram).apply{
    this["colorTexture"]?.set(Texture2D(gl, "media/flame.png"))
  }
  val backgroundMaterial = Material(backgroundProgram).apply{
    this["colorTexture"]?.set(Texture2D(gl, "media/space.png"))
  }
  val laserMaterial = Material(texturedProgram).apply{
    this["colorTexture"]?.set(Texture2D(gl, "media/laser.png"))
  }
  val asteroidMaterial = Material(texturedProgram).apply{
    this["colorTexture"]?.set(Texture2D(gl, "media/asteroid.png"))
  }
  val ufoMaterial = Material(texturedProgram).apply{
    this["colorTexture"]?.set(Texture2D(gl, "media/ufo.png"))
  }

  val texturedQuadGeometry = TexturedQuadGeometry(gl)
  val backgroundMesh = Mesh(backgroundMaterial, texturedQuadGeometry)
  val fighterMesh = Mesh(fighterMaterial, texturedQuadGeometry)
  val thrusterMesh = Mesh(thrusterMaterial, texturedQuadGeometry)
  val laserMesh = Mesh(laserMaterial, texturedQuadGeometry)
  val asteroidMesh = Mesh(asteroidMaterial, texturedQuadGeometry)
  val ufoMesh = Mesh(ufoMaterial, texturedQuadGeometry)

  val camera = OrthoCamera(*Program.all).apply {
    position.set(1f, 1f)
    windowSize.set(20f, 20f)
    updateViewProjMatrix()
  }

  val gameObjects = ArrayList<GameObject>()

  val laser = object : GameObject(laserMesh) {

    override fun move (
      dt : Float,
      t : Float,
      keysPressed : Set<String>,
      gameObjects : List<GameObject>
    ) : Boolean {
      
      position += velocity * dt;
      
      return true;
    }
  }

  val asteroid = object : GameObject(asteroidMesh) {

    override fun move (
      dt : Float,
      t : Float,
      keysPressed : Set<String>,
      gameObjects : List<GameObject>
    ) : Boolean {

      var drag : Float = 0.3f;
      velocity = velocity * exp(-drag * dt);
      
      position += velocity * dt;
      
      return true;
    }
  }

  val ufo = object : GameObject(ufoMesh) {

    override fun move (
      dt : Float,
      t : Float,
      keysPressed : Set<String>,
      gameObjects : List<GameObject>
    ) : Boolean {

      var drag : Float = 0.3f;
      velocity = velocity * exp(-drag * dt);
      
      position += velocity * dt;
      
      return true;
    }
  }

  val avatar = object : GameObject(fighterMesh) {

    val acceleration = Vec3()
    var drag : Float = 0.7f;

    var angularVelocity : Float = 0.5f;
    val angularDrag : Float = 0.7f;

    override fun move (
      dt : Float,
      t : Float,
      keysPressed : Set<String>,
      gameObjects : List<GameObject>
      ) : Boolean {

        if ("q" in keysPressed) {
          angularVelocity -= 10.0f * dt
        }

        if ("e" in keysPressed) {
          angularVelocity += 10.0f * dt
        }

        acceleration.set();
        if ("w" in keysPressed) {
          val modelSpaceAcceleration = Vec3(10f, 0f, 0f)
          acceleration.set(
             Vec4(modelSpaceAcceleration, 0f) * modelMatrix
          )
        }

        velocity = velocity + (acceleration * dt);
        position += velocity * dt;

        velocity = velocity * exp(-drag * dt);

        angularVelocity *= exp(-angularDrag * dt)
        roll += angularVelocity * dt

        return true
      }
  }

  init {
    gameObjects += GameObject(backgroundMesh).apply {
      id = "background"
    };
    gameObjects += GameObject(thrusterMesh).apply {
      id = "mainThruster"
      parent = avatar;
      roll = 4.7f;
      position.set(-1.2f)
    };
    gameObjects += GameObject(thrusterMesh).apply {
      id = "leftThruster"
      parent = avatar;
      roll = 4.2f;
      position.set(-1f, 1.0f)
    };
    gameObjects += GameObject(thrusterMesh).apply {
      id = "rightThruster"
      parent = avatar;
      roll = 5.1f;
      position.set(-1f, -1.0f)
    };
    gameObjects += avatar.apply {
      id = "avatar"
    }
    gameObjects += asteroid.apply {
      id = "asteroid1"
      position.set(-2f, -2f)
    }
    gameObjects += ufo.apply {
      id = "ufo1"
      position.set(2f, 2f)
    }
  }

  fun resize(canvas : HTMLCanvasElement) {
    gl.viewport(0, 0, canvas.width, canvas.height)//#viewport# tell the rasterizer which part of the canvas to draw to ˙HUN˙ a raszterizáló ide rajzoljon
    camera.setAspectRatio(canvas.width.toFloat()/canvas.height)
  }

  val timeAtFirstFrame = Date().getTime()
  var timeAtLastFrame =  timeAtFirstFrame

  var canShoot = true;
  var timeSinceLastShot : Float = 0.0f;

  @Suppress("UNUSED_PARAMETER")
  fun update(keysPressed : Set<String>) {
    val timeAtThisFrame = Date().getTime() 
    val dt = (timeAtThisFrame - timeAtLastFrame).toFloat() / 1000.0f
    val t = (timeAtThisFrame - timeAtFirstFrame).toFloat() / 1000.0f
    
    timeAtLastFrame = timeAtThisFrame

    if (canShoot && " " in keysPressed) {

      gameObjects += laser.apply {
        id = "laser"
        roll = avatar.roll + 1.5f;
        position.x = avatar.position.x;
        position.y = avatar.position.y;
        val xFloat : Float = cos((roll - 1.5).toFloat()) * 8.0f;
        val yFloat : Float = sin((roll - 1.5).toFloat()) * 8.0f;
        velocity = Vec3(xFloat, yFloat, 0.0f);
      };

      canShoot = false;
    }

    if (!canShoot) {
      timeSinceLastShot += dt;
    }

    if (timeSinceLastShot > 3.0) {
      canShoot = true;
      timeSinceLastShot = 0.0f;
    }

    gameObjects.forEach {
      it.move(dt, t, keysPressed, gameObjects)
    }
    
    camera.position.set(avatar.position)
    camera.updateViewProjMatrix()
   
    gl.clearColor(0.3f, 0.0f, 0.3f, 1.0f)//## red, green, blue, alpha in [0, 1]
    gl.clearDepth(1.0f)//## will be useful in 3D ˙HUN˙ 3D-ben lesz hasznos
    gl.clear(GL.COLOR_BUFFER_BIT or GL.DEPTH_BUFFER_BIT)//#or# bitwise OR of flags

    gl.enable(GL.BLEND)
    gl.blendFunc(
      GL.SRC_ALPHA,
      GL.ONE_MINUS_SRC_ALPHA)

    gameObjects.forEach {
      it.update()
    }

    gameObjects.forEach {
      if (
        !("w" in keysPressed) && it.id == "mainThruster" ||
        !("q" in keysPressed) && it.id == "leftThruster" ||
        !("e" in keysPressed) && it.id == "rightThruster"
      ) {
        // do nothing
      } else {
        it.draw(this, camera)
      }
    }

    gameObjects.forEach { obj1 ->
      gameObjects.forEach { obj2 ->
        if (
          obj1 == obj2 ||
          obj1.id ==  "laser" || obj2.id == "laser" ||
          obj1.id ==  "background" || obj2.id == "background" ||
          obj1.parent != null || obj2.parent != null
        ) {
          return@forEach;
        }

        // calculate the distance
        val dist = obj1.position - obj2.position; 
        val distance : Float = sqrt((obj2.position.x - obj1.position.x).pow(2.0f) + (obj2.position.y - obj1.position.y).pow(2.0f))

        var radius = 0.75f;

        val normal : Vec3 = dist / distance;
        
        // if they are not touching exit loop
        if (distance > (radius * 2)) {
          return@forEach;
        }

        val relVelocity = obj1.velocity - obj2.velocity;
        val dotProduct = normal.dot(relVelocity)
        var impMag = dotProduct / 1.5f

        val temp = normal * (impMag * -1.0f)

        // adjust along normal
        obj1.position += normal * dt
        obj2.position -= normal * dt

        obj1.velocity = obj1.velocity + temp;
        obj2.velocity = obj2.velocity - temp;
      }
    }
  }
}
