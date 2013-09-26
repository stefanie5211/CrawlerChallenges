function info = ParseLine(tline)
% To parse each item of the given line
% Explanation about data item
% SunSPOT:"spot's extended ieee address" Content,"accelX,accelY,accelZ,tempC,tempF,"
% SunSPOT:0014.4F01.0000.464B,-0.07250268528464017,0.024167561761546726,0.9801288936627283,24.9,76.82,

%if ~strcmp(tline(13:20), 'SunSPOT:')
%    error('Wrong Format of the Line')
%end

st = 9 + 12;
ed = 27 + 12;
ieeeAddr = tline(st:ed);
idx = findstr(ieeeAddr, '.');
ID = hex2dec(ieeeAddr(idx(end)+1:end));

itemName = { 'time','tempC', 'tempF'};
cmd = ['info = struct(''ID'',',num2str(ID), ','];

%info.ieeeAddr = tline(st:ed);
stIdx = findstr(tline, ',') + 1;
edIdx = [ stIdx(2:end)-2, length(tline) ];


for i = 1:length(itemName)
    cmd = [cmd, ...
        '''', itemName{i}, ''',', tline( stIdx(i):edIdx(i) ), ','];
end
cmd(end) = [];
cmd = [cmd, ');'];
disp(cmd);
eval(cmd);
end