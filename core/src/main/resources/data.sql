-- Insert teams
INSERT INTO teams (name, description) VALUES ('개발팀', 'Software Development Team');
INSERT INTO teams (name, description) VALUES ('디자인팀', 'UI/UX Design Team');
INSERT INTO teams (name, description) VALUES ('마케팅팀', 'Marketing Team');

-- Insert users with team relationships
INSERT INTO users (name, email, team_id) VALUES ('김철수', 'kim@example.com', 1);
INSERT INTO users (name, email, team_id) VALUES ('이영희', 'lee@example.com', 1);
INSERT INTO users (name, email, team_id) VALUES ('박민수', 'park@example.com', 2);
INSERT INTO users (name, email, team_id) VALUES ('최지은', 'choi@example.com', 2);
INSERT INTO users (name, email, team_id) VALUES ('정다은', 'jung@example.com', 3);
