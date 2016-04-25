import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.util.Iterator;
import java.util.Arrays;

public class Poste {
  public static class TokenizerMapper extends Mapper<Object,Text,Text,Text>{
    private Text word = new Text();
    public void map(Object key,Text value, Context context) throws IOException,InterruptedException{
      String localite = value[8];
      String wgps = value[10];
      context.write(localite,wgps);
    }
  }
}
