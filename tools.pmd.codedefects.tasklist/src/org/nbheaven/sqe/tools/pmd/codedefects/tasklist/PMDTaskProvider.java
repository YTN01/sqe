/* Copyright 2005,2006 Sven Reimers, Florian Vogler
 *
 * This file is part of the Software Quality Environment Project.
 *
 * The Software Quality Environment Project is free software:
 * you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation,
 * either version 2 of the License, or (at your option) any later version.
 *
 * The Software Quality Environment Project is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.nbheaven.sqe.tools.pmd.codedefects.tasklist;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.sourceforge.pmd.RuleViolation;
import org.nbheaven.sqe.tools.pmd.codedefects.core.PMDResult;
import org.nbheaven.sqe.tools.pmd.codedefects.core.PMDResult.ClassKey;
import org.nbheaven.sqe.tools.pmd.codedefects.core.PMDSession;
import org.nbheaven.sqe.core.java.search.SearchUtilities;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.tasklist.PushTaskScanner;
import org.netbeans.spi.tasklist.Task;
import org.netbeans.spi.tasklist.TaskScanningScope;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Sven Reimers
 */
public final class PMDTaskProvider extends PushTaskScanner {
        
    public PMDTaskProvider() {
        super( "PMD", "PMD found Errors", null);
    }    
    
    public synchronized void setScope(TaskScanningScope taskScanningScope, Callback callback) {
        if (taskScanningScope == null || callback == null)
            return ;
        
        for (FileObject file : taskScanningScope.getLookup().lookupAll(FileObject.class)) {
            Project project = FileOwnerQuery.getOwner(file);
            if(null != project && null != project.getLookup().lookup(PMDSession.class) && null != JavaSource.forFileObject(file)) {
                PMDResult result = getResult(file);
                Map<Object, Collection<RuleViolation>> instanceByClass = result.getInstanceByClass();
                Collection<String> classes = SearchUtilities.getFQNClassNames(file);
                List<Task> tasks = new LinkedList<Task>();
                for(String className: classes) {
                    for (Object key: instanceByClass.keySet()) {
                        ClassKey classKey = (ClassKey) key;
                        if (classKey.getClassName().equals(className)) {
                            Collection<RuleViolation> bugs = instanceByClass.get(classKey);
                            tasks.addAll(getTasks(bugs, file));
                        }
                    }
                }
                callback.setTasks(file, tasks);
            }
        }
        
        for (Project project : taskScanningScope.getLookup().lookupAll(Project.class)) {
            if (null != project.getLookup().lookup(PMDSession.class)) {
                PMDResult result = getResult(project);
                if (result != null) {
                    List<Task> tasks = new LinkedList<Task>();
                    for (Map.Entry<Object, Collection<RuleViolation>> classKey: result.getInstanceByClass().entrySet()) {
                        tasks.addAll(getTasks(classKey.getValue(), ((PMDResult.ClassKey) classKey.getKey()).getFileObject()));
                    }
                    callback.setTasks(project.getProjectDirectory(), tasks);
                }
            }
        }    
        
    }

    private List<Task> getTasks(Collection<RuleViolation> bugs, FileObject file) {
        if (file == null) {
            return Collections.emptyList();
        }
        List<Task> tasks = new LinkedList<Task>();
        for(RuleViolation ruleViolation: bugs) {
            tasks.add(Task.create(file, "sqe-tasklist-pmd", ruleViolation.getDescription(), ruleViolation.getBeginLine()));                                
        }
        return tasks;
    } 
    
    private PMDResult getResult(FileObject fileObject) {
        return getResult(FileOwnerQuery.getOwner(fileObject));        
    }
    
    private PMDResult getResult(Project project) {
        PMDSession qualitySession = project.getLookup().lookup(PMDSession.class);
        PMDResult result = qualitySession.getResult();
        if (null == result) {
            result = qualitySession.computeResultAndWait();
        }            
        return result;
    }
    
}    
