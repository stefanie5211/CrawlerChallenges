%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% This is a simple demo to analyze coming data files quasi real time
% check the length of the data file, if new data come in, plot them 
% on screen.
% Author: Yuting Zhang 09/19/2012
% Modified by David Hughes 11/1/2012
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

clc;clear;close all;
period = 1/5; % P
oldLen = 0;
newTrace = [];
count = 0;  % initialize data count
%num = 0;   % record number of while loop iterations
p = [];     % handle for free range plot only

% % Graphical Interface
% % Initial Setup 
grid on;
grid minor;

while (true)
    %num = num + 1;
    [status, oldLen, newTrace]= CheckFile('./DataFile.txt', oldLen);
    % status == 0 means ok
    pause(period)
    
    % file open error
    if status == 1 
        disp('Cannot find the file...')
        continue;   % skip rest of loop, go back to start of while
    % no update
    elseif status == 2 
        % disp('There is no update about data file.')
        continue;   % skip rest of loop, go back to start of while
    % first time to check this file
    end
    disp('There is an update.')

    % Count and display new data.
    for i = 1:length(newTrace)
        %time = newTrace(i).time; 
        %id1 = newTrace(i).ID1;
        dist1 = newTrace(i).dist1;
        %id2 = newTrace(i).ID2;
        dist2 = newTrace(i).dist2;
        %id3 = newTrace(i).ID3;
        dist3 = newTrace(i).dist3;
        %id4 = newTrace(i).ID4;
        dist4 = newTrace(i).dist4;
        count = count+1;
        disp(['data update number: ', num2str(count)]),
        disp(newTrace(1,i));       
    end
    
    % Convert strings to double
    distance1 = str2double(dist1);
    distance2 = str2double(dist2);
    distance3 = str2double(dist3);
    distance4 = str2double(dist4);
    
    % Create a matrix of the distances.
    %   | x  | y  | distance(m) |
    B = [ 0    0    distance1;
          0    390  distance2;
          390  390  distance3;
          390  0    distance4];

    % Get the Free Range coordinates
    if (~isempty(p)) 
        delete(p);
    end
    [FRx, FRy,p] = trilateration(B,4);
    disp(['Free Range x: ',num2str(FRx),', y: ',num2str(FRy)]);
     
end
