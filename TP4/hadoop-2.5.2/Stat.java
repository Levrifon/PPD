import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.lang.Integer;
import java.lang.Float;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Stat {

   public static class AgeMapper
   extends Mapper<Object, Text, IntWritable, FloatWritable> {
    public void map(Object key, Text value, Context context) throws IOException,InterruptedException {
      String[] line = value.toString().split(",");
      IntWritable age;
      FloatWritable salary;
      /* on ignore les libellés dans notre calcul*/
      if(line[1].equals("age") || line[4].equals("income")) {
        return;
      }

      int age_value = Integer.parseInt(line[1]);
      float salary_value = Float.parseFloat(line[4]);
      age = new IntWritable();
      salary = new FloatWritable();

      age.set(age_value);
      salary.set(salary_value);

      context.write(age, salary);
    }
   }

  public static class StatReducer
  extends Reducer<IntWritable, FloatWritable, IntWritable, Text> {
    public void reduce(IntWritable key, Iterable<FloatWritable> values, Context context) throws IOException, InterruptedException {

      int nbSalarie = 0;
      float minSalary = 0, maxSalary = 0, avgSalary = 0, sumSalary = 0;
      double standardDeviation = 0, sumSalaryPow = 0;
      boolean first = true;
      StringBuilder res = new StringBuilder();
      Iterator<FloatWritable> it = values.iterator();
      FloatWritable value;

      while(it.hasNext()) {
        value = it.next();

        if(first) {
          maxSalary = value.get();
          minSalary = value.get();
        }

        if(value.get() < minSalary) { /*actualisation du salaire minimum*/
          minSalary = value.get();
        }

        if(value.get() > maxSalary) { /* actualisation du salaire maximum */
          maxSalary = value.get();
        }

        sumSalary += value.get();
        sumSalaryPow = Math.pow(value.get(), 2);
        nbSalarie++;
      }

      avgSalary = sumSalary / nbSalarie;
      /* (1/n)*sum*avgSalary**2*/
      standardDeviation = (1.0/nbSalarie) * sumSalaryPow * Math.pow(avgSalary,2.0);

      res.append("Nombre personnes: ");
      res.append(nbSalarie);
      res.append(", Salaire min: ");
      res.append(minSalary);
      res.append(", Salaire max: ");
      res.append(maxSalary);
      res.append(" Salaire moyen: ");
      res.append(avgSalary);
      res.append(", difference: ");
      res.append(standardDeviation);

      context.write(key, new Text(res));
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "bank");
    job.setJarByClass(Stat.class);
    job.setMapperClass(AgeMapper.class);
    job.setReducerClass(StatReducer.class);
    job.setOutputKeyClass(IntWritable.class);
    job.setOutputValueClass(FloatWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
