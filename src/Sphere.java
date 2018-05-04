// Sphere class
// defines a Sphere shape

import javax.vecmath.*;

public class Sphere extends Shape {
	private Vector3f center;	// center of sphere
	private float radius;		// radius of sphere

	public Sphere() {
	}
	public Sphere(Vector3f pos, float r, Material mat) {
		center = new Vector3f(pos);
		radius = r;
		material = mat;
	}

    public HitRecord hit(Ray ray, float tmin, float tmax) {
        /* YOUR WORK HERE: complete the sphere's intersection routine */
        Vector3f origin = new Vector3f(ray.getOrigin());
        Vector3f direction = new Vector3f(ray.getDirection());
        origin.sub(center);

        float a = direction.lengthSquared();
        float b = 2* origin.dot(direction);
        float c = origin.lengthSquared() - (radius * radius);
        float root_value = (float) (b * b - 4.0 * a * c);
        float sqRoot = (float) Math.sqrt(root_value);

        float t;
        float t1 = (-b + sqRoot) / (2.0f * a);
        float t2 = (-b - sqRoot) / (2.0f * a);
        if(root_value < 0)
            return null;
        else if(root_value == 0){
            if (t1 >= 0 && t1 >= tmin && t1 <= tmax )
                t = t1;
            else
                return null;
        }
        else {
            if (t1 > tmin && t1 <= tmax) {
                if (tmin<=t2 && t2<=tmax && t2 < t1)
                    t=t2;
                else
                    t = t1;
            } else if (tmin<=t2 && t2<=tmax)
                t = t2;
             else
                return null;
        }
        HitRecord rec = new HitRecord();
        rec.pos = ray.pointAt(t);
        rec.t = t;
        rec.material = material;
        Vector3f normal = new Vector3f(ray.pointAt(t));
        normal.sub(center);
        normal.scale((float)1/normal.length());
        rec.normal = normal;
        rec.normal.normalize();
        return rec;
    }
}
