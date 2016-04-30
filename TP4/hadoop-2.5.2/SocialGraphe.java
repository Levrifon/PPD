import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.join.TupleWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

public class SocialGraph {

  public static class UserMapper
  extends Mapper<Object, Text,Text, Text> {
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      String[] line = value.toString().split(" ");
      String res;
      IntWritable first = new IntWritable();
      IntWritable second = new IntWritable();

      for(int i = 1; i < line.length; i++) {
        res = new String();

        if(Integer.parseInt(line[0]) > Integer.parseInt(line[i])) {
          first = new IntWritable(Integer.parseInt(line[i]));
          second = new IntWritable(Integer.parseInt(line[0]));
        }
        if(Integer.parseInt(line[0]) < Integer.parseInt(line[i])) {
          first = new IntWritable(Integer.parseInt(line[0]));
          second = new IntWritable(Integer.parseInt(line[i]));
        }


        for(int j = 1; j < line.length; j++) {
          if(j != i) {
            res += line[j] + "|";
          }
        }

        context.write(new Text(new String("("+first.get()+", "+second.get()+")")), new Text(res));
      }
    }
  }

  public static class FriendReducer
  extends Reducer<Text, Text, Text, Text> {
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
      Iterator<Text> it = values.iterator();
      Text current, next;
      ArrayList<String> res;
      boolean first = true;
      int nbFriends
      current = new Text();
      next = new Text();

      while(it.hasNext()) {
        res = new ArrayList<String>();
        nbFriends = 0;
        if(first) {
          current = it.next();
          next = it.next();
          first = false;
        }

        String[] csplit = current.toString().split("|");
        String[] nsplit = next.toString().split("|");

        for(int j = 0; j < csplit.length; j++) {
          for(int k = 0; k < nsplit.length; k++) {
            if(csplit[j].equals(nsplit[k]) && (!csplit[j].isEmpty()) && (!nsplit[k].isEmpty())) {
              if(!res.contains(csplit[j])) {
                res.add(csplit[j]);
                nbFriends++;
              }
            }
          }
        }

        StringBuilder finalres = new StringBuilder();
        for(int j = 0; j < res.size(); ++j) {
          finalres.append(res.get(j) + "|");
        }
        context.write(key, new Text(new String("nbFriend: " + nbFriend+" Friends: "+finalres)));
      }
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "social graph");
    job.setJarByClass(SocialGraphe.class);
    job.setMapperClass(UserMapper.class);
    job.setReducerClass(FriendReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
