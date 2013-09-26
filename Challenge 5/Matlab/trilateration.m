
% 2D Trilateration to determine the location of the unknoown node
% Syntax: 
% B - Beacon matrix, 
%       B(i,:) represents a beacon, 
%       B(i,1) is the x coordinate of Beacon i,
%       B(i,2) is the y coordinate of Beacon i,
%       B(i,3) is the (distance) from the unknown node to Beacon i.
% BeaconN - Number of Beacons
% [x, y] - the x, y coordinates of the unknown node
% algorithm: 
% Q = inv(D'*D)*D'*b
% where D = 2* [x1- x2, y1-y2; x1-x3, y1-y3; ...; x1-xn, y1-yn]
%       b = [x1^2-x2^2+y1^2-y2^2+d1^2-d2^2;...;x1^2-xn^2+y1^2-yn^2+d1^2-dn^2]
% Q = [x;y] the coordinates of the unknown node.
% Author:
% Yuting Zhang <ytzhang@bu.edu>
% 10/24/2012

function [x, y, p] = trilateration(B, BeaconN)
D = zeros(BeaconN -1, 2);
b = zeros(BeaconN -1, 1);
for i = 1 : BeaconN -1
    D(i, :) = [ B(1,1) - B(i+1, 1), B(1, 2) - B(i+1, 2) ];
    b(i) = B(1,1)^2 - B(i+1,1)^2 + B(1,2)^2 - B(i+1,2)^2 - B(1,3)^2 + B(i+1,3)^2;
end
D = 2 * D;
DT = D';
Q = (DT * D) \ DT * b;
x = Q(1);
y = Q(2);

% Graphical Interface
axis manual;
axis([0,390,0,390]);
set(gca,'GridLineStyle','-');
line([0,0],[0,390],'LineWidth',3,'Color','c');
line([0,390],[390,390],'LineWidth',3,'Color','c');
line([390,390],[390,0],'LineWidth',2,'Color','c');
line([390,0],[0,0],'LineWidth',2,'Color','c');
text(-15,-15,'Beacon 1','FontSize',15);
text(-15,405,'Beacon 2','FontSize',15);
text(375,405,'Beacon 3','FontSize',15);
text(375,-15,'Beacon 4','FontSize',15);
hold on;
plot([0,0,390,390],... 
     [0,390,390,0],...
     'LineStyle','none',...
     'Marker','s',...
     'MarkerEdgeColor','k',...
     'MarkerFaceColor','r',...
     'MarkerSize',15);
set(gca,'position',[.05,.05,.9,.9]);
set(gcf,'un','n','pos',[0.01,0.06,0.98,0.85]);
title('Localization of a Free RangeSunSpot','FontSize',20);
% Plot the lastest data location of the free range
p = plot(x, y,'-s',...
        'MarkerEdgeColor','k',...
        'MarkerFaceColor','b',...
        'MarkerSize',15);
 
end
