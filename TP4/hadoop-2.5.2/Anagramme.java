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

public class Anagramme {
  public static class TokenizerMapper extends Mapper<Object,Text,Text,Text>{
    private Text word = new Text();
    public void map(Object key,Text value, Context context) throws IOException,InterruptedException{
      /*http://stackoverflow.com/questions/605891/sort-a-single-string-in-java#605901*/

      char[] mytext = value.toString().toLowerCase().toCharArray();
      /*trie la chaine par ordre alphabetique*/
      Arrays.sort(mytext);
      /*la clé correspond au mot Sorté et la valeur au mot de base */
      context.write(new Text(new String(mytext)),value);
    }
  }
  public static class AnagrammeReducer extends Reducer<Text,Text,Text,Text>{
    public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,InterruptedException{
      Iterator<Text> it = values.iterator();
      boolean firstWord = true;
      StringBuilder builder = new StringBuilder();
      while(it.hasNext()){
        Text currentWord = it.next();
        if(firstWord) {
          firstWord = false;
          builder.append(currentWord);
        } else {
          builder.append(";" + currentWord);
        }

      }
      context.write(key,new Text(builder.toString()));
    }
  }
  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "anagramme");
    job.setJarByClass(Anagramme.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(AnagrammeReducer.class);
    job.setReducerClass(AnagrammeReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }

}
