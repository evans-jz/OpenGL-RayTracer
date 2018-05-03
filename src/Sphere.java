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
        float rootValue = (float) (b * b - 4.0 * a * c);
        
        float sqRoot = (float) Math.sqrt(rootValue);
        
        if (rootValue <= 0) {
            return null;
        }

        float temp1 = (-b + sqRoot) / (2.0f * a);
        float temp2 = (-b - sqRoot) / (2.0f * a);

        float temp;

        if(rootValue == 0){
            if ( temp1 >= 0 && temp1 >= tmin && temp1 <= tmax ){
                temp = temp1;
            }
            else
                return null;
        }
        else if(rootValue > 0){
            if (temp1 > tmin && temp1 <= tmax) {
                if (tmin<=temp2 && temp2<=tmax && temp2 < temp1){
                    temp=temp2;
                }else {
                    temp = temp1;
                }
            } else if (tmin<=temp2 && temp2<=tmax) {
                temp = temp2;
            } else {
                return null;
            }
        }
        else
            return null;

        Vector3f normal = new Vector3f(ray.pointAt(temp));
        normal.sub(center);
        HitRecord rec = new HitRecord();
        rec.pos = ray.pointAt(temp);
        rec.t = temp;
        rec.material = material;
        rec.normal = normal;
        rec.normal.normalize();

        return rec;
    }
}
