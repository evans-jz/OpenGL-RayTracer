// RayTracer class

import javax.vecmath.*;
import java.util.*;
import java.awt.*;
import java.awt.image.*;
import javax.imageio.*;
import java.io.*;

public class RayTracer {

	private Color3f image[][];	// image that stores floating point color
	private String image_name;	// output image name
	private int width, height;	// image width, height
	private int xsample, ysample;	// samples used for super sampling
	private Color3f background;	// background color
	private Color3f ambient;	// ambient color
	private int maxdepth;		// max recursion depth for recursive ray tracing
	private float exposure;		// camera exposure for the entire scene

	private Camera camera;
	private Vector<Material> materials = new Vector<Material> ();	// array of materials
	private Vector<Shape> shapes = new Vector<Shape> ();			// array of shapes
	private Vector<Light> lights = new Vector<Light> ();			// array of lights

	private void initialize() {
		width = 256;
		height = 256;
		xsample = 1;
		ysample = 1;
		maxdepth = 5;
		background = new Color3f(0,0,0);
		ambient = new Color3f(0,0,0);
		exposure = 1.0f;

		image_name = new String("output.png");

		camera = new Camera(new Vector3f(0,0,0), new Vector3f(0,-1,0), new Vector3f(0,1,0), 45.f, 1.f);

		// add a default material: diffuse material with constant 1 reflectance
		materials.add(Material.makeDiffuse(new Color3f(0,0,0), new Color3f(1,1,1)));
	}

	public static void main(String[] args) {
		if (args.length == 1) {
			new RayTracer(args[0]);
		} else {
			System.out.println("Usage: java RayTracer input.scene");
		}
	}

	private HitRecord checkIntersection(Ray ray){
		float tmax = Float.MAX_VALUE; //Default t values
		float tmin = (float).0001;
		HitRecord hit = null;
		for (int i = 0; i < shapes.size(); i++){
			HitRecord temp_hit = shapes.get(i).hit(ray, tmin, tmax);
			if(temp_hit!=null){
				tmax = temp_hit.t;
				hit = temp_hit;
			}
		}
		return hit;
	}

	private Color3f raytracing(Ray ray, int depth)
	{
		/* YOUR WORK HERE: complete the ray tracing function
		 * Feel free to make new function as needed. For example, you may want to add a 'shading' function */
		HitRecord hit = checkIntersection(ray);
		if ( hit != null)
			return rayColor(ray, hit, depth);
		else
			return background;
	}
	private Color3f rayColor(Ray ray, HitRecord hit, int depth) {
		Color3f c = new Color3f();
		if (depth == maxdepth)
			return c;
		else if(depth < maxdepth){
			for (int i = 0; i < lights.size();i++){
				Vector3f light_pos = new Vector3f();
				Vector3f light_dir = new Vector3f();
				lights.get(i).getLight(hit.pos, light_pos, light_dir);
				Vector3f temp_light = new Vector3f(light_pos);
				temp_light.sub(hit.pos);
				Ray shadow_ray = new Ray(hit.pos, temp_light);
				HitRecord shad_hit = checkIntersection(shadow_ray);
				if (shad_hit == null || shad_hit.t > temp_light.length())
					c.add(evaluateShadingModel(hit, lights.get(i), ray));
			}

			if (!hit.material.Kr.equals(new Color3f()))
				c.add(reflectRay(hit, ray, depth));
			if(hit.material.ior > 1)
				c.add(refractRay(hit, ray, depth));
		}
		else
			return null;

		Color3f temp_color = new Color3f(hit.material.Ka.x * ambient.x,
				hit.material.Ka.y * ambient.y,
				hit.material.Ka.z * ambient.z);
		c.add(temp_color);
		return c;
	}
	public Color3f reflectRay(HitRecord hit, Ray ray, int depth){
		Color3f c = new Color3f();
		Vector3f refl_direction = new Vector3f(ray.getDirection());
		refl_direction.scale(-1.0f);
		refl_direction = reflect(refl_direction, hit.normal);
		Ray refl_ray = new Ray(hit.pos, refl_direction);
		HitRecord refl_hit = checkIntersection(refl_ray);

		if(refl_hit != null){
			Color3f refl_color = rayColor(refl_ray, refl_hit, depth + 1);
			c.add(new Color3f(hit.material.Kr.x * refl_color.x,
					hit.material.Kr.y * refl_color.y,
					hit.material.Kr.z * refl_color.z));
		} else {
			c.add( new Color3f(hit.material.Kr.x * background.x,
					hit.material.Kr.y * background.y,
					hit.material.Kr.z * background.z));
		}
		return c;
	}
	public Color3f refractRay(HitRecord hit, Ray ray, int depth){
		Color3f c = new Color3f();
		Vector3f orig_normal = new Vector3f(hit.normal);
		Vector3f orig_dir = new Vector3f(ray.getDirection());
		orig_dir.normalize();
		Vector3f refr_dir = refract(orig_dir, orig_normal, hit.material.ior);
		if(refr_dir == null){
			return reflectRay(hit, ray, depth);
		} else {
			Ray refr_ray = new Ray(hit.pos, refr_dir);
			HitRecord refr_hit = checkIntersection(refr_ray);
			float n_i = hit.material.ior;
			float n_t = 1.0f;
			Vector3f R_d = new Vector3f(ray.getDirection());
			R_d.normalize();
			Vector3f R_n = new Vector3f(hit.normal);

			float start = (float) ((Math.pow((n_i - n_t),2)) / (Math.pow((n_i + n_t),2)));
			float middle = 1 - ((float) ((Math.pow((n_i - n_t),2)) / (Math.pow((n_i + n_t),2))));
			float end = (float) Math.pow(1 + R_d.dot(R_n), 5);
			float R = start + (middle*end);

			if(refr_hit != null){
				Color3f rayColor = rayColor(refr_ray, refr_hit, depth + 1);
				Color3f tempColor = new Color3f(hit.material.Kt);
				tempColor.scale(1 - R);

				Color3f finalColor = new Color3f(tempColor.x * rayColor.x,
						tempColor.y * rayColor.y,
						tempColor.z * rayColor.z);
				Color3f reflectColor = reflectRay( hit, ray,depth +1);
				reflectColor.scale(R);
				c.add(reflectColor, finalColor);
				return  c;
			} else {
				Color3f tempColor = new Color3f(hit.material.Kt);
				tempColor.scale(1 - R);

				Color3f finalColor = new Color3f(tempColor.x * background.x,
						tempColor.y * background.y,
						tempColor.z * background.z);
				Color3f reflectColor = reflectRay( hit, ray,depth +1);
				reflectColor.scale(R);
				c.add(reflectColor, finalColor);
				return  c;
			}
		}
	}

	private Color3f evaluateShadingModel(HitRecord hit, Light light, Ray ray) {
		Color3f color = new Color3f();
		Vector3f light_pos = new Vector3f();
		Vector3f light_dir = new Vector3f();
		Color3f lightIntens = light.getLight(hit.pos, light_pos, light_dir);

		if (lightIntens == null)
			return color;
		else {
			Color3f diffuse = new Color3f(hit.material.Kd.x * lightIntens.x,
					hit.material.Kd.y * lightIntens.y,
					hit.material.Kd.z * lightIntens.z);
			Color3f specular = new Color3f(hit.material.Ks.x  * lightIntens.x,
					hit.material.Ks.y * lightIntens.y,
					hit.material.Ks.z * lightIntens.z);
			light_dir.normalize();
			diffuse.scale(Math.max(hit.normal.dot(light_dir), 0));
			color.add(diffuse);
			Vector3f reflected = reflect(light_dir, hit.normal);
			Vector3f opp = new Vector3f(ray.d);
			opp.scale(-1);
			opp.normalize();
			specular.scale((float) Math.pow(Math.max(reflected.dot(opp), 0), hit.material.phong_exp));
			color.add(specular);
			return color;
		}
	}

	// reflect a direction (in) around a given normal
	/* NOTE: dir is assuming to point AWAY from the hit point
	 * if your ray direction is point INTO the hit point, you should flip
	 * the sign of the direction before calling reflect
	 */
	private Vector3f reflect(Vector3f dir, Vector3f normal)
	{
		Vector3f out = new Vector3f(normal);
		out.scale(2.f * dir.dot(normal));
		out.sub(dir);
		return out;
	}

	// refract a direction (in) around a given normal and 'index of refraction' (ior)
	/* NOTE: dir is assuming to point INTO the hit point
	 * (this is different from the reflect function above, which assumes dir is pointing away
	 */
	private Vector3f refract(Vector3f dir, Vector3f normal, float ior)
	{
		float mu;
		mu = (normal.dot(dir) < 0) ? 1.f / ior : ior;

		float cos_thetai = dir.dot(normal);
		float sin_thetai2 = 1.f - cos_thetai*cos_thetai;

		if (mu*mu*sin_thetai2 > 1.f) return null;
		float sin_thetar = mu*(float)Math.sqrt(sin_thetai2);
		float cos_thetar = (float)Math.sqrt(1.f - sin_thetar*sin_thetar);

		Vector3f out = new Vector3f(normal);
		if (cos_thetai > 0)
		{
			out.scale(-mu*cos_thetai+cos_thetar);
			out.scaleAdd(mu, dir, out);

		} else {

			out.scale(-mu*cos_thetai-cos_thetar);
			out.scaleAdd(mu, dir, out);
		}
		out.normalize();
		return out;
	}

	public RayTracer(String scene_name) {

		// initialize and set default parameters
		initialize();

		// parse scene file
		parseScene(scene_name);

		// create floating point image
		image = new Color3f[width][height];

		int i, j;
		float x, y;
		for (j=0; j<height; j++)
		{
			y = (float)j / (float)height;
			System.out.print("\rray tracing... " + j*100/height + "%");
			for (i=0; i<width; i ++)
			{
				x = (float)i / (float)width;
				image[i][j] = raytracing(camera.getCameraRay(x, y), 0);
			}
		}
		System.out.println("\rray tracing completed.                       ");
				
		writeImage();
	}

	private void parseScene(String scene_name)
	{
		File file = null;
		Scanner scanner = null;
		try {
			file = new File(scene_name);
			scanner = new Scanner(file);
		} catch (IOException e) {
			System.out.println("error reading from file " + scene_name);
			System.exit(0);
		}
		String keyword;
		while(scanner.hasNext()) {

			keyword = scanner.next();
			// skip the comment lines
			if (keyword.charAt(0) == '#') {
				scanner.nextLine();
				continue;
			}
			if (keyword.compareToIgnoreCase("image")==0) {

				image_name = scanner.next();
				width = scanner.nextInt();
				height = scanner.nextInt();
				exposure = scanner.nextFloat();

			} else if (keyword.compareToIgnoreCase("camera")==0) {

				Vector3f eye = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
				Vector3f at  = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
				Vector3f up  = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
				float fovy = scanner.nextFloat();
				float aspect_ratio = (float)width / (float)height;

				camera = new Camera(eye, at, up, fovy, aspect_ratio);

			} else if (keyword.compareToIgnoreCase("background")==0) {

				background = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());

			} else if (keyword.compareToIgnoreCase("ambient")==0) { 

				ambient = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());

			} else if (keyword.compareToIgnoreCase("maxdepth")==0) {

				maxdepth = scanner.nextInt();

			} else if (keyword.compareToIgnoreCase("light")==0) {

				// parse light
				parseLight(scanner);

			} else if (keyword.compareToIgnoreCase("material")==0) {

				// parse material
				parseMaterial(scanner);

			} else if (keyword.compareToIgnoreCase("shape")==0) {

				// parse shape
				parseShape(scanner);
		
			} else {
				System.out.println("undefined keyword: " + keyword);
			}
		}
		scanner.close();
	}

	private void parseLight(Scanner scanner)
	{
		String lighttype;
		lighttype = scanner.next();
		if (lighttype.compareToIgnoreCase("point")==0) {

			/* add a new point light */
			Vector3f pos = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f intens = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			lights.add(new PointLight(pos, intens));

		} else if (lighttype.compareToIgnoreCase("spot")==0) {

			/* add a new spot light */
			Vector3f from = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f to = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			float spot_exponent = scanner.nextFloat();
			float spot_cutoff = scanner.nextFloat();
			Color3f intens = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());

			lights.add(new SpotLight(from, to, spot_exponent, spot_cutoff, intens));

		} else if (lighttype.compareToIgnoreCase("area")==0) {
			/* YOUR WORK HERE: complete the area light
			 * Note that you do not need to create a new type of light source.
			 * Instead, you will convert an area light
			 * to a collection of point lights and add them all to the 'lights' array */
			Vector3f center = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			float quad_size = scanner.nextFloat();
			float x =  scanner.nextFloat();
			float y =  scanner.nextFloat();
			Color3f color = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f intensity_point = new Color3f();		//intensity of each point light in the area light
			intensity_point.add(color);
			intensity_point.scale(1/ x / y);
			for(int i = 0; i < x; i++) {
				for(int j = 0; j < y; j++) {
					Vector3f pos = new Vector3f(center.x - quad_size / 2 + quad_size / x * i, center.y, center.z - quad_size / 2 + quad_size / y * j);
					lights.add(new PointLight(pos, intensity_point));
				}
			}
		} else {
			System.out.println("undefined light type: " + lighttype);
		}
	}

	private void parseMaterial(Scanner scanner)
	{
		String mattype;
		mattype = scanner.next();
		if (mattype.compareToIgnoreCase("diffuse")==0) {

			Color3f ka = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f kd = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			materials.add(Material.makeDiffuse(ka, kd));

		} else if (mattype.compareToIgnoreCase("specular")==0) {

			Color3f ka = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f kd = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f ks = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			float phong_exp = scanner.nextFloat();
			materials.add(Material.makeSpecular(ka, kd, ks, phong_exp));

		} else if (mattype.compareToIgnoreCase("mirror")==0) {

			Color3f kr = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			materials.add(Material.makeMirror(kr));

		} else if (mattype.compareToIgnoreCase("glass")==0) {

			Color3f kr = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f kt = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			float ior = scanner.nextFloat();
			materials.add(Material.makeGlass(kr, kt, ior));

		} else if (mattype.compareToIgnoreCase("super")==0) {

			Color3f ka = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f kd = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f ks = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			float phong_exp = scanner.nextFloat();
			Color3f kr = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Color3f kt = new Color3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			float ior = scanner.nextFloat();
			materials.add(Material.makeSuper(ka, kd, ks, phong_exp, kr, kt, ior));			
		}

		else {
			System.out.println("undefined material type: " + mattype);
		}

	}

	private void parseShape(Scanner scanner)
	{
		String shapetype;
		shapetype = scanner.next();
		Material material = materials.lastElement();
		if (shapetype.compareToIgnoreCase("plane")==0) {

			Vector3f P0 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f N = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			shapes.add(new Plane(P0, N, material));

		} else if (shapetype.compareToIgnoreCase("sphere")==0) {

			Vector3f center = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			float radius = scanner.nextFloat();
			shapes.add(new Sphere(center, radius, material));

		} else if (shapetype.compareToIgnoreCase("triangle")==0) {

			Vector3f p0 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f p1 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f p2 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			shapes.add(new Triangle(p0, p1, p2, material));

		} else if (shapetype.compareToIgnoreCase("triangle_n")==0) {

			Vector3f p0 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f p1 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f p2 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());

			Vector3f n0 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f n1 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());
			Vector3f n2 = new Vector3f(scanner.nextFloat(), scanner.nextFloat(), scanner.nextFloat());

			shapes.add(new Triangle(p0, p1, p2, n0, n1, n2, material));

		} else if (shapetype.compareToIgnoreCase("trimesh")==0) {

			TriMesh	mesh = new TriMesh();
			mesh.load(scanner.next());

			if (mesh.type.compareToIgnoreCase("triangle")==0) {
				int i;
				int idx0, idx1, idx2;
				for (i=0; i<mesh.faces.length/3; i++) {
					idx0 = mesh.faces[i*3+0];
					idx1 = mesh.faces[i*3+1];
					idx2 = mesh.faces[i*3+2];
					shapes.add(new Triangle(mesh.verts[idx0], mesh.verts[idx1], mesh.verts[idx2], material));
				}

			} else if (mesh.type.compareToIgnoreCase("triangle_n")==0) {
				int i;
				int idx0, idx1, idx2;
				for (i=0; i<mesh.faces.length/3; i++) {
					idx0 = mesh.faces[i*3+0];
					idx1 = mesh.faces[i*3+1];
					idx2 = mesh.faces[i*3+2];
					shapes.add(new Triangle(mesh.verts[idx0], mesh.verts[idx1], mesh.verts[idx2],
											mesh.normals[idx0], mesh.normals[idx1], mesh.normals[idx2],
											material));
				}

			} else {
				System.out.println("undefined trimesh type: " + mesh.type);
			}


		} else {
			System.out.println("undefined shape type: " + shapetype);
		}
	}

	// write image to a disk file
	// image will be multiplied by exposure
	private void writeImage() {
		int x, y, index;
		int pixels[] = new int[width * height];

		index = 0;
		// apply a standard 2.2 gamma correction
		float gamma = 1.f / 2.2f;
		for (y=height-1; y >= 0; y --) {
			for (x=0; x<width; x ++) {
				Color3f c = new Color3f(image[x][y]);
				c.x = (float)Math.pow(c.x*exposure, gamma);
				c.y = (float)Math.pow(c.y*exposure, gamma);
				c.z = (float)Math.pow(c.z*exposure, gamma);
				c.clampMax(1.f);
				pixels[index++] = c.get().getRGB();

			}
		}

		BufferedImage oimage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		oimage.setRGB(0, 0, width, height, pixels, 0, width);
		File outfile = new File(image_name);
		try {
			ImageIO.write(oimage, "png", outfile);
		} catch(IOException e) {
		}
	}
}
