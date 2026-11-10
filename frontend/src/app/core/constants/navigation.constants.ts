import { NavItem } from '../models/navigation.model';

export const NAVIGATION_ITEMS: readonly NavItem[] = [
  { label: 'Workspace', route: '/dashboard', icon: 'space_dashboard', section: 'core' },
  { label: 'Patients', route: '/patients', icon: 'personal_injury', section: 'clinical', phaseTag: 'Planned' },
  { label: 'Appointments', route: '/appointments', icon: 'event_available', section: 'clinical', phaseTag: 'Planned' },
  { label: 'Doctors', route: '/doctors', icon: 'medical_services', section: 'clinical', phaseTag: 'Planned' },
  { label: 'Admissions', route: '/admissions', icon: 'hotel', section: 'clinical', phaseTag: 'Planned' },
  { label: 'Clinical / EMR', route: '/clinical', icon: 'clinical_notes', section: 'clinical', phaseTag: 'Planned' },
  { label: 'Laboratory', route: '/laboratory', icon: 'biotech', section: 'operations', phaseTag: 'Planned' },
  { label: 'Pharmacy', route: '/pharmacy', icon: 'medication', section: 'operations', phaseTag: 'Planned' },
  { label: 'Inventory', route: '/inventory', icon: 'inventory_2', section: 'operations', phaseTag: 'Planned' },
  { label: 'Billing', route: '/billing', icon: 'receipt_long', section: 'operations', phaseTag: 'Planned' },
  { label: 'Reports', route: '/reports', icon: 'assessment', section: 'operations', phaseTag: 'Planned' },
  { label: 'Employees', route: '/employees', icon: 'badge', section: 'administration', phaseTag: 'Planned' },
  { label: 'Users', route: '/users', icon: 'manage_accounts', section: 'administration', phaseTag: 'Planned' },
  { label: 'Hospital Profile', route: '/hospital', icon: 'domain', section: 'administration', phaseTag: 'Planned' },
];
