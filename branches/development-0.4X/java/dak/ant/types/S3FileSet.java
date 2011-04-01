package dak.ant.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.selectors.AndSelector;
import org.apache.tools.ant.types.selectors.DateSelector;
import org.apache.tools.ant.types.selectors.FileSelector;
import org.apache.tools.ant.types.selectors.FilenameSelector;
import org.apache.tools.ant.types.selectors.MajoritySelector;
import org.apache.tools.ant.types.selectors.NoneSelector;
import org.apache.tools.ant.types.selectors.NotSelector;
import org.apache.tools.ant.types.selectors.OrSelector;
import org.apache.tools.ant.types.selectors.PresentSelector;
import org.apache.tools.ant.types.selectors.SelectSelector;
import org.apache.tools.ant.types.selectors.SelectorUtils;
import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Object;

import dak.ant.selectors.S3KeySelector;

public class S3FileSet extends DataType 
       { // INSTANCE VARIABLES
    
         private String             bucket;
         private String             prefix;
         private List<S3File>       files                = new ArrayList<S3File>();
         private PatternSet         defaultPatterns      = new PatternSet();
         private List<PatternSet>   additionalPatterns   = new ArrayList<PatternSet>  ();
         private List<FileSelector> selectors            = new ArrayList<FileSelector>();
         private boolean            errorOnMissingBucket = true;
         
         private List<S3File>       included;

         // TASK ATTRIBUTES
    
         public void setBucket(String bucket) 
                { if (isReference())
                    throw tooManyAttributes();

                  this.bucket   = bucket;
                  this.included = null;
                  
                  for (S3File file: files)
                      file.setBucket(bucket);
                }

         /** Returns the S3 bucket attribute, dereferencing it if required.
           * 
           */
         public String getBucket() 
                { if (isReference())
                     return getRef(getProject()).getBucket();

                  dieOnCircularReference();

                  return bucket;
                }
         
         public void setPrefix(String prefix) 
                { if (isReference())
                    throw tooManyAttributes();

                  this.prefix   = prefix;
                  this.included = null;
                }

         /** Returns the S3 prefix attribute, dereferencing it if required.
           * 
           */
         public String getPrefix() 
                { if (isReference())
                     return getRef(getProject()).getPrefix();

                  dieOnCircularReference();

                  return prefix;
                }

         /** Sets whether an error is thrown if a bucket does not exist. Default value
           * is <code>true</code>.
           * 
           * @param enabled <code>true</code> if missing buckets cause errors.
           */
         public void setErrorOnMissingBucket(boolean enabled) 
                { if (isReference())
                     throw tooManyAttributes();

                  this.errorOnMissingBucket = enabled;
                  this.included             = null;
                }

         /** Returns the 'fail on error' task attribute value, dereferencing it if required.
           * 
           */
         public boolean getErrorOnMissingBucket() 
                { if (isReference())
                     return getRef(getProject()).getErrorOnMissingBucket();

                  dieOnCircularReference();

                  return errorOnMissingBucket;
                }

         // PATTERN ATTRIBUTES
         
         /**  Creates a nested patternset.
          * 
           * @return <code>PatternSet</code>.
           */
         public synchronized PatternSet createPatternSet() 
                { if (isReference()) 
                     { throw noChildrenAllowed();
                     }
             
                  PatternSet patterns = new PatternSet();
             
                  additionalPatterns.add(patterns);

                  this.included = null;
             
                  return patterns;
                }
         
         /** Appends <code>includes</code> to the current list of include patterns.
           * <p>
           * Patterns may be separated by a comma or a space.
           * </p>
           * 
           * @param includes the <code>String</code> containing the include patterns.
           */
         public synchronized void setIncludes(String includes) 
                { if (isReference())
                     throw tooManyAttributes();

                  this.defaultPatterns.setIncludes(includes);
                  this.included = null;
                }

         /** Appends <code>excludes</code> to the current list of exclude patterns.
           * <p>
           * Patterns may be separated by a comma or a space.
           * </p>
           * 
           * @param excludes the <code>String</code> containing the exclude patterns.
           */
         public synchronized void setExcludes(String excludes) 
                { if (isReference())
                     throw tooManyAttributes();

                  this.defaultPatterns.setExcludes(excludes);
                  this.included = null;
                }


         /** create&lt;Type&gt; implementation for an included <code>S3File</code>.
           * 
           */
         public S3File createS3File() 
                { if (isReference())
                     throw noChildrenAllowed();

                  S3File file = new S3File();

                  file.setBucket(this.bucket);
                  files.add     (file);
                  
                  this.included = null;

                  return file;
                }
    
         // TESTED SELECTORS

         public void addFilename(FilenameSelector selector) 
                { appendSelector(selector);
                }

         public void addKey(S3KeySelector selector) 
                { appendSelector(selector);
                }

         public void addDate(DateSelector selector) 
                { appendSelector(selector);
                }

         public void addAnd(AndSelector selector) 
                { appendSelector(selector);
                }

         public void addOr(OrSelector selector) 
                 { appendSelector(selector);
                 }

         public void addNot(NotSelector selector) 
                { appendSelector(selector);
                }

         public void addNone(NoneSelector selector) 
                { appendSelector(selector);
                }

         public void addSelector(SelectSelector selector) 
                { appendSelector(selector);
                }

         public void addMajority(MajoritySelector selector) 
                { appendSelector(selector);
                }

         public void addPresent(PresentSelector selector) 
                { appendSelector(selector);
                }

         public synchronized void appendSelector(FileSelector selector) 
                { if (isReference()) 
                     { throw noChildrenAllowed();
                     }
                 
                  selectors.add(selector);
                }
         
         // IMPLEMENTATION

         /** Makes this instance in effect a reference to another instance.
           *
           * <p>You must not set another attribute or nest elements inside
           * this element if you make it a reference.</p>
           * @param r the <code>Reference</code> to use.
           * @throws BuildException on error
           */
         @Override
         public void setRefid(Reference r) throws BuildException 
                { if ((bucket != null) || defaultPatterns.hasPatterns(getProject())) 
                     throw tooManyAttributes();
                
                  if (!additionalPatterns.isEmpty()) 
                     throw noChildrenAllowed();

                  if (!selectors.isEmpty()) 
                     throw noChildrenAllowed();
    
                  super.setRefid(r);
                }
         
         public Iterator<S3File> iterator(S3Service service) 
                { if (isReference()) 
                     return ((S3FileSet) getCheckedRef(getProject())).iterator(service);

                  calculateSet(service);

                  return included.iterator();
                }

         public int size(S3Service service) 
                { if (isReference()) 
                     return ((S3FileSet) getCheckedRef(getProject())).size(service);

                  calculateSet(service);
    
                  return included.size();
                }

         /** Performs the check for circular references and returns the referenced
           * S3FileSet.
           * 
           */
         private S3FileSet getRef(Project project) 
                 { return (S3FileSet) getCheckedRef(project);
                 }

         private synchronized void calculateSet(S3Service service) 
                 { checkParameters();

                   // ... cached ?
                 
                   if (service == null)
                      throw new BuildException("Uninitialized S3 service");
                   
                   if (included != null)
                      return;

                   // ... scan and select 
                   
                   included = new ArrayList<S3File>();

                   try 
                      { Set<S3File> objects = scan(getProject(),service);
                        
                        for (S3File object: objects) 
                            { if (isSelected(object.getKey(),object)) 
                                 { included.add(object);
                                 }
                            }
                      } 
                   catch(BuildException x)
                      { throw x;
                      }
                   catch (Exception x) 
                      { throw new BuildException(x);
                      }
                 }

         private void checkParameters() throws BuildException 
                 { if (bucket == null)
                      throw new BuildException("Missing 'bucket' attribute");
                 }

         private boolean isSelected(String name,S3File file)
                 { File basedir = new File("");

                   for (int i=0; i<selectors.size(); i++)
                       { if (!((FileSelector) selectors.get(i)).isSelected(basedir,name,file)) 
                            { return false;
                            }
                       }
        
                   return true;
                 }
                 
         private Set<S3File> scan(Project project,S3Service service) 
                 { Set<S3File> included = new ConcurrentSkipListSet<S3File>();

                   try 
                      { // ... validate

                        if (service.getBucket(bucket) == null)
                           { if (errorOnMissingBucket)
                                throw new BuildException("S3 bucket '" + bucket + "' does not exist");
                           
                             return included;
                           }

                        // ... initialise
                        
                        PatternSet ps       = mergePatterns(project);
                        String[]   explicit = keys(files);
                        String[]   includes = normalize(ps.getIncludePatterns(project));
                        String[]   excludes = normalize(ps.getExcludePatterns(project));

                        // ... set include/exclude lists

                        if (explicit == null)
                           explicit = new String[0];

                        if (includes == null)
                           includes = (explicit.length == 0) ? new String[] { SelectorUtils.DEEP_TREE_MATCH } : new String[0];

                         if (excludes == null)
                            excludes = new String[0];

                         // ... scan object list

                         S3Object[] list;

                         if (prefix != null)
                            list = service.listObjects(bucket, prefix, null);
                            else
                            list = service.listObjects(bucket);

                         for (S3Object object: list) 
                             { String  key      = object.getKey(); 
                               boolean selected = false;
                               boolean include  = false;
                               boolean exclude  = false;

                               // ... hack to get wildcard match on objects in the root of the bucket 
                               //     (e.g. includes="**/xxx.bak" when xxx.bak is in the bucket root)
                               
                               if (!key.startsWith(".") && !key.startsWith("/"))
                                  key = "/" + key;                                  

                               for (String pattern: explicit) 
                                   { if (SelectorUtils.match(pattern, key))
                                        selected = true;
                                   }

                               for (String pattern: includes) 
                                   { if (SelectorUtils.match(pattern, key))
                                        include = true;
                                   }

                               for (String pattern: excludes) 
                                   { if (SelectorUtils.match(pattern, key))
                                        exclude = true;
                                   }

                               if (selected || (include && !exclude))
                                  included.add(new S3File(object));
                             }
                         
                         return included;
                      } 
                   catch (BuildException x) 
                      { throw x;
                      } 
                   catch (Exception x) 
                      { throw new BuildException(x);
                      }
                 }

         /** Converts a array of S3File to the equivalent list of S3 object keys.
           * 
           */
         private static String[] keys(List<S3File> files) 
                 { String[] keys  = new String[files == null ? 0 : files.size()];
                   int      index = 0;

                   if (files != null)
                     { for (S3File file: files)
                           { String key = file.getKey();
                           
                             if (key.startsWith(".") || key.startsWith("/"))
                                keys[index++] = file.getKey();
                                else
                                keys[index++] = "/" + file.getKey();
                           }
                     }

                  return keys;
                }
         
         /** Normalises a list of include/exclude patterns to use. All '\' characters are replaced
           * by <code>/</code> to match the S3 convention.
           * <p>
           * When a pattern ends with a '/' or '\', "**" is appended.
           * 
           * @param includes A list of include patterns. May be <code>null</code>,
           *                indicating that all objects should be included. If a non-
           *                <code>null</code> list is given, all elements must be non-
           *                <code>null</code>.
           */
         private static String[] normalize(String[] patterns) 
                 { if (patterns == null) 
                      return null;

                   String[] normalized = new String[patterns.length];

                   for (int i=0; i<patterns.length; i++) 
                       { normalized[i] = normalize(patterns[i]);
                       }
                  
                   return normalized;
                 }
         
         /** All '/' and '\' characters are replaced by <code>/</code> to match the S3 storage convention.
           * <p>
           * When a pattern ends with a '/' or '\', "**" is appended.
           * 
           */
         private static String normalize(String pattern) 
                 { String string = pattern.replace('\\', '/');
 
                   if (string.endsWith("/")) 
                      { string += SelectorUtils.DEEP_TREE_MATCH;
                      }
 
                   return string;
                 }
   
         /** Get the merged patterns for this objectset.
           * 
           */
         public synchronized PatternSet mergePatterns(Project project) 
                { PatternSet ps = (PatternSet) defaultPatterns.clone();

                  for (PatternSet item : additionalPatterns) 
                      { ps.append(item, project);
                      }

                  return ps;
                }
         
         
         // *** UNTESTED SELECTORS ***


//         @Override
//         public void addSize(SizeSelector selector)   
//                { appendSelector(selector);
//                }
//
//         @Override
//         public void addType(TypeSelector selector) 
//                { appendSelector(selector);
//                }
//
//         @Override
//         public void addCustom(ExtendSelector selector) 
//                { appendSelector(selector);
//                }
//
//         @Override
//         public void addDepth(DepthSelector selector) 
//                { appendSelector(selector);
//                }
//
//         @Override
//         public void addDepend(DependSelector selector) 
//                { appendSelector(selector);
//                }
//
//         @Override
//         public void addModified(ModifiedSelector selector) 
//                { appendSelector(selector);
//                }
//
//         @Override
//         public void add(FileSelector selector) 
//                { appendSelector(selector);
//                }
//
//         @Override
//         public void addContains(ContainsSelector selector) 
//                { // ignore, won't work
//                }
//
//         @Override
//         public void addContainsRegexp(ContainsRegexpSelector selector) 
//                { // ignore, won't work
//                }
//
//         @Override
//         public void addDifferent(DifferentSelector selector) 
//                { // ignore, won't work
//                }
//
//         @Override
//         public FileSelector[] getSelectors(Project aProject) 
//                { return (FileSelector[]) selectors.toArray();
//                }
//
//         @Override
//         public boolean hasSelectors() 
//                { return selectors.size() != 0;
//                }
//
//         @Override
//         public int selectorCount() 
//                { return selectors.size();
//                }
//
//         @SuppressWarnings({ "rawtypes" })
//         @Override
//         public Enumeration selectorElements() 
//                { return Collections.enumeration(selectors);
//                }
       }
