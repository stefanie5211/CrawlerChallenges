%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% This is a simple demo to analyze coming data files quasi real time
% check the length of the data file, if new data come in, plot them on
% screen
% author:
% Yuting Zhang 09/19/2012
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

clc;clear;close all;
period = 1/5; % P
% Store the Last line. Find the Content that is appended. And Plot them
oldLen = 0;
newTrace = [];
IDSet = []; % Set of Node ID
stateSet = [];
count = 0; % initialize data count
j=1;
g=0;
value1=1;
value2=2;
%M1=struct('tempC',value1,'time',value2);
%M2=struct(field1,value1,field2,value2);

num = 0;

x = [0,200,400,600,800];
y = 26:30;
hLine1 = plot(x,y);
hold on;
hLine2 = plot(x,y);
set(hLine2,'Color','red','LineWidth',1);
%StripChart('Initialize',gca)
while 1
    num = num + 1;
    %disp('check file length')
    [status, oldLen, newTrace]= CheckFile('./SPOTAccelTempData2.txt', oldLen);
    % status == 0 means ok;
    % status == 1 means file open error
    % status == 2 means no update
    % status == 3 means first time to check this file.
    pause(period)
    if status == 1
        disp('cannot find the file...')
        continue;
    elseif status == 2
        %disp('there is no update')
        continue;
    elseif status == 3
        continue;
    end
    disp('there is update')
    
    allID = [newTrace.ID];
    uSet = unique(allID); % Get the unique ID that has sent data.
    newID = setdiff(uSet, IDSet); % Get new ID that just start started
    IDSet = [IDSet, newID]; % Update ID set
    stateSet{ length(IDSet) } = []; % Initialize the state for NewID.
    
 
    
   
    
    disp(['Length of NewTrace: ', num2str(length(newTrace))])
    for i = 1:length(newTrace)
        id = newTrace(i).ID; % Get the source ID of this data gram.
        

        
        Temp = [newTrace(i).tempC, newTrace(i).tempF];
   
        switch id
            case 13711
        %ts1 = ts1.addsample('Time',newTrace(i).time,'Data',newTrace(i).tempC);
        StripChart('Update',hLine1,newTrace(i).tempC);
        
        %time_unix = newTrace(i).time; % example time
        %time_reference = datenum('1970', 'yyyy'); 
        %time_matlab = time_reference + time_unix / 8.64e7;
        %time_matlab_string = datestr(time_matlab, 'HH:MM:SS');
       
        %M1(j).tempC=newTrace(i).tempC;
        %M1(j).time=time_matlab_string;
      
        %if(count<5)
        %ts1 = timeseries(M1(j).tempC,M1(j).time);
       % plot(ts1);
        
        %  j=j+1;
        
        %else ts1 = timeseries(M1,count-5:count);
        %end
        
            case 18014
        %ts2 = ts2.addsample('Time',newTrace(i).time,'Data',newTrace(i).tempC);
        %M2=[M2,newTrace(i).tempC,newTrace(i).time];
        StripChart('Update',hLine2,newTrace(i).tempC);
        
        %time_unix = newTrace(i).time; % example time
        %time_reference = datenum('1970', 'yyyy'); 
        %time_matlab = time_reference + time_unix / 8.64e7;
        %time_matlab_string = datestr(time_matlab, 'HH:MM:SS.FFF');
        %if(count<5)
        %ts2 = timeseries(M2,0:count);
        
       % else ts2 = timeseries(M2,count-5:count);
       % end
   
         %   otherwise 
         %       disp('wrong id');
        end 
            
        
        

        %hLine = plot(newTrace(i).time,newTrace(i).tempC);
        %StripChart('Initialize',gca)
        %StripChart('Update',hLine,newTrace(i).tempC)

        count = count+1;
        disp(['Coming data updated: ',num2str(count)]),
        disp(newTrace(1,i));
               
      
    end
        %plot(ts1);
        %hold on;
        %p=plot(ts2);
        xlabel('time/milisecond');
        ylabel('temper C');
       title('time-temper');
       % set(p,'Color','red','LineWidth',1);
       legend('id=358f','id=465e');
    
    
end